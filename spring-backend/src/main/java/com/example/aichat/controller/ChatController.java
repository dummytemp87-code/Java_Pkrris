package com.example.aichat.controller;

import com.example.aichat.dto.ChatReplyDto;
import com.example.aichat.dto.ChatRequestDto;
import com.example.aichat.dto.MessageDto;
import com.example.aichat.model.User;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.service.OpenAIService;
import com.example.aichat.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai-chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final OpenAIService openAIService;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public ChatController(OpenAIService openAIService, UserRepository userRepository, SubscriptionService subscriptionService) {
        this.openAIService = openAIService;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chat(@AuthenticationPrincipal UserDetails principal, @RequestBody ChatRequestDto req) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (!subscriptionService.isEntitled(user)) {
            return ResponseEntity.status(402).body(Map.of(
                    "error", "Your trial has ended. Upgrade to keep using AI features.",
                    "code", "SUBSCRIPTION_REQUIRED"
            ));
        }
        try {
            List<Map<String, String>> msgs = new ArrayList<>();
            String system = (req.getSystemPrompt() == null || req.getSystemPrompt().isBlank())
                    ? "You are a friendly, student-focused AI tutor. Write answers in clean Markdown (headings, lists, bold) with step-by-step clarity. Do NOT use LaTeX or $...$ or \\(...\\) or \\[...\\]. Use plain-text math: exponents with ^ (x^2), fractions as a/b, inline code for short expressions (like `x^2 + 1`). Keep responses concise unless asked for more detail and include small, relevant examples."
                    : req.getSystemPrompt();
            msgs.add(Map.of("role", "system", "content", system));

            if (req.getMessages() != null) {
                for (MessageDto m : req.getMessages()) {
                    if (m.getText() == null || m.getText().isBlank()) continue;
                    String role = "user";
                    if ("tutor".equalsIgnoreCase(m.getRole())) role = "assistant";
                    else if ("user".equalsIgnoreCase(m.getRole())) role = "user";
                    msgs.add(Map.of("role", role, "content", m.getText()));
                }
            }

            String reply = openAIService.getChatCompletion(msgs);
            return ResponseEntity.ok(new ChatReplyDto(reply));
        } catch (Exception ex) {
            log.error("Failed to get AI response", ex);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Failed to get AI response");
            return ResponseEntity.status(500).body(err);
        }
    }
}
