package com.example.aichat.controller;

import com.example.aichat.dto.SubscribeRequest;
import com.example.aichat.model.Referral;
import com.example.aichat.model.Subscription;
import com.example.aichat.model.User;
import com.example.aichat.repo.ReferralRepository;
import com.example.aichat.repo.SubscriptionRepository;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.service.RazorpayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final RazorpayService razorpayService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${razorpay.plan.starter:}")
    private String starterPlanId;

    @Value("${razorpay.plan.pro:}")
    private String proPlanId;

    public BillingController(SubscriptionRepository subscriptionRepository, UserRepository userRepository, ReferralRepository referralRepository, RazorpayService razorpayService) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.referralRepository = referralRepository;
        this.razorpayService = razorpayService;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> status(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        Subscription sub = subscriptionRepository.findByUser(user).orElse(null);
        if (sub == null) {
            return ResponseEntity.ok(Map.of("plan", "NONE", "status", "NONE"));
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("plan", sub.getPlan());
        body.put("status", sub.getStatus());
        body.put("trialEndsAt", sub.getTrialEndsAt());
        body.put("currentPeriodEnd", sub.getCurrentPeriodEnd());
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/subscribe", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> subscribe(@AuthenticationPrincipal UserDetails principal, @RequestBody SubscribeRequest req) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        String plan = Optional.ofNullable(req.getPlan()).orElse("").trim().toLowerCase();
        String planId;
        if ("starter".equals(plan)) {
            planId = starterPlanId;
        } else if ("pro".equals(plan)) {
            planId = proPlanId;
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "plan must be 'starter' or 'pro'"));
        }
        if (planId == null || planId.isBlank()) {
            return ResponseEntity.status(500).body(Map.of("error", "Billing is not configured yet"));
        }

        try {
            String subscriptionId = razorpayService.createSubscription(planId, user.getId());

            Subscription sub = subscriptionRepository.findByUser(user).orElseGet(() -> {
                Subscription s = new Subscription();
                s.setUser(user);
                return s;
            });
            sub.setRazorpaySubscriptionId(subscriptionId);
            sub.setPlan(plan.toUpperCase());
            sub.setUpdatedAt(Instant.now());
            subscriptionRepository.save(sub);

            return ResponseEntity.ok(Map.of(
                    "subscriptionId", subscriptionId,
                    "keyId", razorpayService.getKeyId()
            ));
        } catch (Exception ex) {
            log.error("Failed to create Razorpay subscription", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to start checkout"));
        }
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> webhook(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                      @RequestBody String rawBody) {
        if (!razorpayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Rejected webhook call with invalid signature");
            return ResponseEntity.status(400).body(Map.of("error", "Invalid signature"));
        }

        try {
            JsonNode root = mapper.readTree(rawBody);
            String event = root.path("event").asText("");
            JsonNode subEntity = root.path("payload").path("subscription").path("entity");
            String razorpaySubscriptionId = subEntity.path("id").asText(null);
            if (razorpaySubscriptionId == null) {
                return ResponseEntity.ok(Map.of("ok", true)); // nothing to do for this event shape
            }

            Subscription sub = subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubscriptionId).orElse(null);
            if (sub == null) {
                log.warn("Webhook for unknown subscription id: {}", razorpaySubscriptionId);
                return ResponseEntity.ok(Map.of("ok", true));
            }

            switch (event) {
                case "subscription.activated":
                case "subscription.charged": {
                    sub.setStatus("ACTIVE");
                    long currentEnd = subEntity.path("current_end").asLong(0);
                    if (currentEnd > 0) sub.setCurrentPeriodEnd(Instant.ofEpochSecond(currentEnd));
                    creditReferrerIfApplicable(sub.getUser());
                    break;
                }
                case "subscription.cancelled":
                    sub.setStatus("CANCELLED");
                    break;
                case "subscription.halted":
                    sub.setStatus("PAST_DUE");
                    break;
                default:
                    // Other lifecycle events (e.g. subscription.pending, subscription.completed) -- no local state change needed yet.
                    break;
            }
            sub.setUpdatedAt(Instant.now());
            subscriptionRepository.save(sub);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            log.error("Failed to process Razorpay webhook", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process webhook"));
        }
    }

    /**
     * Credits a referrer with a free month (extends their local entitlement window by 30
     * days) the first time their referred friend successfully pays. Capped at 6/year per
     * referrer, and only ever fires once per referral (status flips PENDING -> REWARDED),
     * so repeat "subscription.charged" events on renewal don't re-credit.
     */
    private void creditReferrerIfApplicable(User referredUser) {
        Referral referral = referralRepository.findByReferredUser(referredUser).orElse(null);
        if (referral == null || !"PENDING".equals(referral.getStatus())) return;

        User referrer = referral.getReferrer();
        long rewardsThisYear = referralRepository.countByReferrerAndStatusAndCreatedAtAfter(
                referrer, "REWARDED", Instant.now().minus(365, ChronoUnit.DAYS));
        if (rewardsThisYear >= 6) {
            log.info("Referrer {} hit the 6/year reward cap, not crediting", referrer.getId());
            return;
        }

        Subscription referrerSub = subscriptionRepository.findByUser(referrer).orElse(null);
        if (referrerSub != null) {
            if ("TRIALING".equals(referrerSub.getStatus()) && referrerSub.getTrialEndsAt() != null) {
                referrerSub.setTrialEndsAt(referrerSub.getTrialEndsAt().plus(30, ChronoUnit.DAYS));
            } else if (referrerSub.getCurrentPeriodEnd() != null) {
                referrerSub.setCurrentPeriodEnd(referrerSub.getCurrentPeriodEnd().plus(30, ChronoUnit.DAYS));
            } else {
                referrerSub.setStatus("TRIALING");
                referrerSub.setTrialEndsAt(Instant.now().plus(30, ChronoUnit.DAYS));
            }
            subscriptionRepository.save(referrerSub);
        }

        referral.setStatus("REWARDED");
        referral.setRewardedAt(Instant.now());
        referralRepository.save(referral);
    }
}
