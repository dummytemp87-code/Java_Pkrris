package com.example.aichat.controller;

import com.example.aichat.dto.ArticleRequest;
import com.example.aichat.model.ArticleContent;
import com.example.aichat.model.User;
import com.example.aichat.repo.ArticleContentRepository;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.service.OpenAIService;
import com.example.aichat.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private static final Logger log = LoggerFactory.getLogger(ArticleController.class);

    private final ArticleContentRepository articleRepo;
    private final UserRepository userRepository;
    private final OpenAIService aiService;
    private final SubscriptionService subscriptionService;

    public ArticleController(ArticleContentRepository articleRepo, UserRepository userRepository, OpenAIService aiService, SubscriptionService subscriptionService) {
        this.articleRepo = articleRepo;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping(value = "/content", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> content(@AuthenticationPrincipal UserDetails principal, @RequestBody ArticleRequest req) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            String goalTitle = Optional.ofNullable(req.getGoalTitle()).orElse("").trim();
            String moduleTitle = Optional.ofNullable(req.getModuleTitle()).orElse("").trim();
            if (goalTitle.isEmpty() || moduleTitle.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "goalTitle and moduleTitle are required"));
            }

            Optional<ArticleContent> existing = articleRepo.findByUserAndGoalTitleAndModuleTitle(user, goalTitle, moduleTitle);
            if (existing.isPresent()) {
                return ResponseEntity.ok(Map.of("content", existing.get().getContentMarkdown()));
            }

            if (!subscriptionService.isEntitled(user)) {
                return ResponseEntity.status(402).body(Map.of(
                        "error", "Your trial has ended. Upgrade to generate new content.",
                        "code", "SUBSCRIPTION_REQUIRED"
                ));
            }

            List<Map<String, String>> messages = new ArrayList<>();
            String system = "You are a helpful tutor. Write clear, engaging educational content as clean Markdown ONLY (no code fences). Use headings, lists, examples, and explanations. Do NOT use LaTeX or $...$. Use plain-text math (x^2, a/b). Length ~500-800 words.";
            messages.add(Map.of("role", "system", "content", system));
            String userPrompt = String.format("Write a tutorial-style article for the module '%s' under the goal '%s'. Include definitions, concepts, step-by-step examples, common pitfalls, and a brief summary.", moduleTitle, goalTitle);
            messages.add(Map.of("role", "user", "content", userPrompt));

            String content = aiService.getChatCompletion(messages);
            String cleaned = content.trim();
            if (cleaned.startsWith("```") ) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n", "");
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            }

            try {
                ArticleContent ac = new ArticleContent();
                ac.setUser(user);
                ac.setGoalTitle(goalTitle);
                ac.setModuleId(req.getModuleId());
                ac.setModuleTitle(moduleTitle);
                ac.setContentMarkdown(cleaned);
                articleRepo.save(ac);
            } catch (DataIntegrityViolationException dup) {
                Optional<ArticleContent> ex2 = articleRepo.findByUserAndGoalTitleAndModuleTitle(user, goalTitle, moduleTitle);
                if (ex2.isPresent()) return ResponseEntity.ok(Map.of("content", ex2.get().getContentMarkdown()));
            }

            return ResponseEntity.ok(Map.of("content", cleaned));
        } catch (Exception ex) {
            log.error("Failed to load article", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load article"));
        }
    }
}
