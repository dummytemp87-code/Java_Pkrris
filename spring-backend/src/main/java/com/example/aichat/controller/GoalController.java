package com.example.aichat.controller;

import com.example.aichat.dto.CreateGoalRequest;
import com.example.aichat.dto.GoalDto;
import com.example.aichat.model.Goal;
import com.example.aichat.model.User;
import com.example.aichat.repo.GoalRepository;
import com.example.aichat.repo.StudyPlanRepository;
import com.example.aichat.repo.ArticleContentRepository;
import com.example.aichat.repo.VideoContentRepository;
import com.example.aichat.repo.QuizContentRepository;
import com.example.aichat.repo.ModuleCompletionLogRepository;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.service.OpenAIService;
import com.example.aichat.service.SubscriptionService;
import com.example.aichat.service.SyllabusTextExtractor;
import com.example.aichat.service.UnsupportedSyllabusFormatException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private static final Logger log = LoggerFactory.getLogger(GoalController.class);

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final ArticleContentRepository articleContentRepository;
    private final VideoContentRepository videoContentRepository;
    private final QuizContentRepository quizContentRepository;
    private final ModuleCompletionLogRepository moduleCompletionLogRepository;
    private final OpenAIService aiService;
    private final SyllabusTextExtractor syllabusTextExtractor;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    public GoalController(GoalRepository goalRepository, UserRepository userRepository, StudyPlanRepository studyPlanRepository, ArticleContentRepository articleContentRepository, VideoContentRepository videoContentRepository, QuizContentRepository quizContentRepository, ModuleCompletionLogRepository moduleCompletionLogRepository, OpenAIService aiService, SyllabusTextExtractor syllabusTextExtractor, SubscriptionService subscriptionService) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.articleContentRepository = articleContentRepository;
        this.videoContentRepository = videoContentRepository;
        this.quizContentRepository = quizContentRepository;
        this.moduleCompletionLogRepository = moduleCompletionLogRepository;
        this.aiService = aiService;
        this.syllabusTextExtractor = syllabusTextExtractor;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Goal> goals = goalRepository.findByUserOrderByCreatedAtDesc(user);
        LocalDate today = LocalDate.now();
        List<GoalDto> dtos = goals.stream().map(g -> new GoalDto(
                g.getId(),
                g.getTitle(),
                g.getProgress(),
                g.getTargetDate() != null ? (int) Math.max(0, ChronoUnit.DAYS.between(today, g.getTargetDate())) : 0
        )).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails principal, @RequestBody CreateGoalRequest req) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            String title = Optional.ofNullable(req.getTitle()).orElse("").trim();
            if (title.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));

            Goal g = new Goal();
            g.setUser(user);
            g.setTitle(title);
            g.setDescription(req.getDescription());
            if (req.getTargetDate() != null && !req.getTargetDate().isBlank()) {
                g.setTargetDate(LocalDate.parse(req.getTargetDate()));
            }
            g.setProgress(0);
            if (req.getTopics() != null && !req.getTopics().isEmpty()) {
                try {
                    String topicsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req.getTopics());
                    g.setTopicsJson(topicsJson);
                } catch (Exception ignore) {}
            }
            Goal saved = goalRepository.save(g);

            LocalDate today = LocalDate.now();
            int daysLeft = saved.getTargetDate() != null ? (int) Math.max(0, ChronoUnit.DAYS.between(today, saved.getTargetDate())) : 0;
            return ResponseEntity.ok(new GoalDto(saved.getId(), saved.getTitle(), saved.getProgress(), daysLeft));
        } catch (DataIntegrityViolationException dup) {
            return ResponseEntity.status(409).body(Map.of("error", "Goal with this title already exists"));
        } catch (Exception ex) {
            log.error("Failed to create goal", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create goal"));
        }
    }

    @PostMapping(value = "/analyze-syllabus", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyzeSyllabus(@AuthenticationPrincipal UserDetails principal, @RequestParam("file") MultipartFile file) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            if (!subscriptionService.isEntitled(user)) {
                return ResponseEntity.status(402).body(Map.of(
                        "error", "Your trial has ended. Upgrade to analyze more syllabuses.",
                        "code", "SUBSCRIPTION_REQUIRED"
                ));
            }
            if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));

            String filename = file.getOriginalFilename();
            String system = "You are an assistant that reads a course syllabus and extracts structured study information. " +
                    "Return STRICT JSON only (no markdown, no prose). Schema: {\"title\": string, \"description\": string, \"topics\": string[]}. " +
                    "title: a concise course/goal title (max 8 words). description: a 1-2 sentence summary of what the course covers. " +
                    "topics: 5-15 distinct topic names covered, in the order they appear.";

            String content;
            if (SyllabusTextExtractor.isImage(filename)) {
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", system));
                messages.add(Map.of("role", "user", "content", "This image shows a course syllabus. Read it and extract the structured information."));
                content = aiService.getChatCompletionWithImage(messages, file.getBytes(), SyllabusTextExtractor.imageMimeType(filename));
            } else {
                String text;
                try {
                    text = syllabusTextExtractor.extract(file);
                } catch (UnsupportedSyllabusFormatException ex) {
                    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
                }
                if (text == null || text.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Could not extract any readable text from the file"));
                }
                // Cap prompt size: a full textbook-length syllabus doesn't need to be sent in full
                // for the model to identify title/description/topics from the opening sections.
                String truncated = text.length() > 12000 ? text.substring(0, 12000) : text;

                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", system));
                messages.add(Map.of("role", "user", "content", "Syllabus content:\n" + truncated));
                content = aiService.getChatCompletion(messages);
            }
            String cleaned = content.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n", "");
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            }

            JsonNode node = mapper.readTree(cleaned);
            Map<String, Object> result = new HashMap<>();
            result.put("title", node.path("title").asText(""));
            result.put("description", node.path("description").asText(""));
            List<String> topics = new ArrayList<>();
            if (node.path("topics").isArray()) {
                for (JsonNode t : node.path("topics")) {
                    String s = t.asText("").trim();
                    if (!s.isBlank()) topics.add(s);
                }
            }
            result.put("topics", topics);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            log.error("Failed to analyze syllabus", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to analyze syllabus. You can still fill in the details manually."));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable("id") Long id) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            Goal g = goalRepository.findById(id).orElse(null);
            if (g == null || !g.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404).body(Map.of("error", "Goal not found"));
            }
            // delete related study plan for this goal if any
            studyPlanRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            // delete related article content for this goal if any
            articleContentRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            // delete related video content for this goal if any
            videoContentRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            // delete related quiz content for this goal if any
            quizContentRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            // delete module completion logs for this goal
            moduleCompletionLogRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            goalRepository.delete(g);
            entityManager.flush();
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            log.error("Failed to delete goal", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete goal"));
        }
    }

    @PostMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<?> deleteFallback(@AuthenticationPrincipal UserDetails principal, @PathVariable("id") Long id) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            Goal g = goalRepository.findById(id).orElse(null);
            if (g == null || !g.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404).body(Map.of("error", "Goal not found"));
            }
            // Cleanup related content for this user's goal title
            studyPlanRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            articleContentRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            videoContentRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            quizContentRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            moduleCompletionLogRepository.deleteByUserIdAndGoalTitle(user.getId(), g.getTitle());
            goalRepository.delete(g);
            entityManager.flush();
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            log.error("Failed to delete goal", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete goal"));
        }
    }
}
