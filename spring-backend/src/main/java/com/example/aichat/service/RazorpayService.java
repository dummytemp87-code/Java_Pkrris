package com.example.aichat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpayService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${razorpay.keyId:}")
    private String keyId;

    @Value("${razorpay.keySecret:}")
    private String keySecret;

    @Value("${razorpay.webhookSecret:}")
    private String webhookSecret;

    /**
     * Creates a Razorpay subscription for the given plan and returns its id.
     * The frontend uses this id with Razorpay's Checkout.js widget to collect payment;
     * the webhook (not this call) is the source of truth for when it actually activates.
     */
    public String createSubscription(String planId, Long userId) throws Exception {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("Missing RAZORPAY_KEY_ID/RAZORPAY_KEY_SECRET. Set the environment variables.");
        }
        String url = "https://api.razorpay.com/v1/subscriptions";

        Map<String, Object> payload = new HashMap<>();
        payload.put("plan_id", planId);
        payload.put("customer_notify", 1);
        payload.put("total_count", 12);
        payload.put("notes", Map.of("userId", String.valueOf(userId)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(keyId, keySecret);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Razorpay error creating subscription: " + resp.getStatusCode());
        }
        JsonNode root = mapper.readTree(resp.getBody());
        String subscriptionId = root.path("id").asText(null);
        if (subscriptionId == null) {
            throw new RuntimeException("Razorpay response missing subscription id");
        }
        return subscriptionId;
    }

    public String getKeyId() {
        return keyId;
    }

    /**
     * Verifies the X-Razorpay-Signature header against the raw webhook body using
     * HMAC-SHA256 with the webhook secret. This is the ONLY authentication on the
     * webhook endpoint (it's permitAll -- Razorpay's servers call it directly, no
     * user JWT is involved), so a failed/missing signature must always be rejected.
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank() || signature == null || payload == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : computed) hex.append(String.format("%02x", b));
            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            return false;
        }
    }
}
