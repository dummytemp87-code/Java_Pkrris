package com.example.aichat.controller;

import com.example.aichat.dto.AuthRequest;
import com.example.aichat.dto.AuthResponse;
import com.example.aichat.dto.RegisterRequest;
import com.example.aichat.dto.ChangePasswordRequest;
import com.example.aichat.model.Referral;
import com.example.aichat.model.Subscription;
import com.example.aichat.model.User;
import com.example.aichat.repo.ReferralRepository;
import com.example.aichat.repo.SubscriptionRepository;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.security.JwtService;
import com.example.aichat.service.EmailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String REFERRAL_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I to avoid confusion
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(10);
    private static final Duration OTP_RESEND_COOLDOWN = Duration.ofSeconds(30);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ReferralRepository referralRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthController(UserRepository userRepository,
                          SubscriptionRepository subscriptionRepository,
                          ReferralRepository referralRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          AuthenticationManager authenticationManager,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.referralRepository = referralRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    private String generateReferralCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(REFERRAL_CODE_CHARS.charAt(RANDOM.nextInt(REFERRAL_CODE_CHARS.length())));
            String code = sb.toString();
            if (userRepository.findByReferralCode(code).isEmpty()) return code;
        }
        // Astronomically unlikely to exhaust 5 attempts at 32^6 possibilities, but fall back to a longer code rather than fail.
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(REFERRAL_CODE_CHARS.charAt(RANDOM.nextInt(REFERRAL_CODE_CHARS.length())));
        return sb.toString();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }
        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setReferralCode(generateReferralCode());
        u = userRepository.save(u);

        // A referred signup gets a longer trial (7 days instead of 3) as their half of the
        // double-sided referral reward; the referrer's reward is credited later, only once
        // this new user actually converts to paid (see BillingController's webhook handler).
        // Redemptions per code are capped -- not a fix for trial abuse in general (there's no
        // email verification in this app, so fake-account trial farming is already possible
        // regardless of referrals), just a ceiling against one code being blasted publicly and
        // farmed at scale.
        User referrer = null;
        String incomingCode = req.getReferralCode();
        if (incomingCode != null && !incomingCode.isBlank()) {
            User candidate = userRepository.findByReferralCode(incomingCode.trim().toUpperCase()).orElse(null);
            if (candidate != null && referralRepository.countByReferrer(candidate) < 100) {
                referrer = candidate;
            }
        }
        int trialDays = referrer != null ? 7 : 3;

        Subscription sub = new Subscription();
        sub.setUser(u);
        sub.setPlan("TRIAL");
        sub.setStatus("TRIALING");
        sub.setTrialEndsAt(Instant.now().plus(trialDays, ChronoUnit.DAYS));
        subscriptionRepository.save(sub);

        if (referrer != null) {
            Referral referral = new Referral();
            referral.setReferrer(referrer);
            referral.setReferredUser(u);
            referral.setStatus("PENDING");
            referralRepository.save(referral);
        }

        String token = jwtService.generateToken(u.getEmail(), u.getTokenVersion() != null ? u.getTokenVersion() : 0);
        return ResponseEntity.ok(new AuthResponse(token, u.getName(), u.getEmail(), u.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
        String token = jwtService.generateToken(user.getEmail(), user.getTokenVersion() != null ? user.getTokenVersion() : 0);
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail(), user.getRole()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", user.getName());
        body.put("email", user.getEmail());
        body.put("role", user.getRole());
        body.put("referralCode", user.getReferralCode());
        body.put("emailVerified", Boolean.TRUE.equals(user.getEmailVerified()));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/send-verification-otp")
    public ResponseEntity<?> sendVerificationOtp(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return ResponseEntity.ok(Map.of("message", "Email already verified", "emailVerified", true));
        }
        Instant lastSent = user.getVerificationOtpSentAt();
        if (lastSent != null && Duration.between(lastSent, Instant.now()).compareTo(OTP_RESEND_COOLDOWN) < 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", "Please wait before requesting another code"));
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setVerificationOtp(passwordEncoder.encode(otp));
        user.setVerificationOtpExpiresAt(Instant.now().plus(OTP_VALIDITY));
        user.setVerificationOtpSentAt(Instant.now());
        userRepository.save(user);

        try {
            emailService.sendOtpEmail(user.getEmail(), user.getName(), otp);
        } catch (Exception ex) {
            log.error("Failed to send verification OTP to {}", user.getEmail(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to send verification email"));
        }
        return ResponseEntity.ok(Map.of("message", "Verification code sent"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@AuthenticationPrincipal UserDetails principal, @RequestBody Map<String, String> body) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return ResponseEntity.ok(Map.of("message", "Email already verified", "emailVerified", true));
        }
        String otp = body != null ? body.get("otp") : null;
        if (otp == null || otp.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Verification code is required"));
        }
        if (user.getVerificationOtp() == null || user.getVerificationOtpExpiresAt() == null
                || Instant.now().isAfter(user.getVerificationOtpExpiresAt())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Code expired -- request a new one"));
        }
        if (!passwordEncoder.matches(otp.trim(), user.getVerificationOtp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid code"));
        }
        user.setEmailVerified(true);
        user.setVerificationOtp(null);
        user.setVerificationOtpExpiresAt(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Email verified", "emailVerified", true));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody ChangePasswordRequest req) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Current password is incorrect"));
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        // rotate token version to sign-out all existing tokens
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userRepository.save(user);
        String token = jwtService.generateToken(user.getEmail(), user.getTokenVersion());
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail(), user.getRole()));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userRepository.save(user);
        String token = jwtService.generateToken(user.getEmail(), user.getTokenVersion());
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail(), user.getRole()));
    }
}
