package com.example.aichat.controller;

import com.example.aichat.dto.AuthRequest;
import com.example.aichat.dto.AuthResponse;
import com.example.aichat.dto.RegisterRequest;
import com.example.aichat.dto.ChangePasswordRequest;
import com.example.aichat.model.User;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000"})
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
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
        u = userRepository.save(u);
        String token = jwtService.generateToken(u.getEmail(), u.getTokenVersion() != null ? u.getTokenVersion() : 0);
        return ResponseEntity.ok(new AuthResponse(token, u.getName(), u.getEmail(), u.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        if (!auth.isAuthenticated()) {
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
        return ResponseEntity.ok(Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()
        ));
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
