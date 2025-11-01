package com.example.aichat.controller;

import com.example.aichat.dto.QuizGenerateRequest;
import com.example.aichat.dto.QuizSubmitRequest;
import com.example.aichat.model.Goal;
import com.example.aichat.model.QuizContent;
import com.example.aichat.model.StudyPlan;
import com.example.aichat.model.User;
import com.example.aichat.repo.GoalRepository;
import com.example.aichat.repo.QuizContentRepository;
import com.example.aichat.repo.StudyPlanRepository;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.service.OpenAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.time.LocalDate;
import com.example.aichat.model.ModuleCompletionLog;
import com.example.aichat.repo.ModuleCompletionLogRepository;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = {"http://localhost:3000"})
public class QuizController {

    private final OpenAIService aiService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final QuizContentRepository quizRepo;
    private final UserRepository userRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final GoalRepository goalRepository;
    private final ModuleCompletionLogRepository moduleCompletionLogRepository;

    public QuizController(OpenAIService aiService,
                          QuizContentRepository quizRepo,
                          UserRepository userRepository,
                          StudyPlanRepository studyPlanRepository,
                          GoalRepository goalRepository,
                          ModuleCompletionLogRepository moduleCompletionLogRepository) {
        this.aiService = aiService;
        this.quizRepo = quizRepo;
        this.userRepository = userRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.goalRepository = goalRepository;
        this.moduleCompletionLogRepository = moduleCompletionLogRepository;
    }

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generate(@AuthenticationPrincipal UserDetails principal, @RequestBody QuizGenerateRequest req) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            String goalTitle = Optional.ofNullable(req.getGoalTitle()).orElse("").trim();
            String moduleTitle = Optional.ofNullable(req.getModuleTitle()).orElse("").trim();
            if (goalTitle.isEmpty() || moduleTitle.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "goalTitle and moduleTitle are required"));
            }

            Optional<QuizContent> existing = quizRepo.findByUserAndGoalTitleAndModuleTitle(user, goalTitle, moduleTitle);
            if (existing.isPresent()) {
                String quizJson = existing.get().getQuizJson();
                try {
                    JsonNode node = mapper.readTree(quizJson);
                    return ResponseEntity.ok(Map.of("quiz", node));
                } catch (Exception ignore) {
                    return ResponseEntity.ok(Map.of("quizText", quizJson));
                }
            }

            // Build prompt
            List<Map<String, String>> messages = new ArrayList<>();
            String system = "You are a quiz generator for study modules. Return STRICT JSON only (no markdown). Schema: {\n" +
                    "  \"title\": string,\n" +
                    "  \"questions\": [ { \"id\": number, \"question\": string, \"options\": [string], \"correctIndex\": number, \"explanation\": string } ]\n" +
                    "}. Create 5 questions. Keep options concise. No extra text.";
            messages.add(Map.of("role", "system", "content", system));

            StringBuilder prompt = new StringBuilder(String.format(
                    "Create a 5-question multiple-choice quiz for the module '%s' under goal '%s'.",
                    moduleTitle, goalTitle));
            // Optionally enrich with goal description/topics if we can find the goal
            Goal goal = goalRepository.findByUserAndTitle(user, goalTitle).orElse(null);
            if (goal != null) {
                String description = Optional.ofNullable(goal.getDescription()).orElse("").trim();
                if (!description.isBlank()) prompt.append("\\nContext/Description: ").append(description);
                String topicsJson = goal.getTopicsJson();
                if (topicsJson != null && !topicsJson.isBlank()) {
                    try {
                        JsonNode tn = mapper.readTree(topicsJson);
                        if (tn.isArray() && tn.size() > 0) {
                            List<String> topics = new ArrayList<>();
                            for (JsonNode n : tn) { String t = n.asText("").trim(); if (!t.isBlank()) topics.add(t); }
                            if (!topics.isEmpty()) {
                                prompt.append("\\nFocus on topics: ").append(String.join(", ", topics));
                            }
                        }
                    } catch (Exception ignore) {}
                }
            }
            messages.add(Map.of("role", "user", "content", prompt.toString()));

            String content = aiService.getChatCompletion(messages);
            String cleaned = content.trim();
            if (cleaned.startsWith("```") ) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n", "");
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            }

            // Persist quiz
            QuizContent qc = new QuizContent();
            qc.setUser(user);
            qc.setGoalTitle(goalTitle);
            qc.setModuleId(req.getModuleId());
            qc.setModuleTitle(moduleTitle);
            qc.setQuizJson(cleaned);
            try {
                quizRepo.save(qc);
            } catch (DataIntegrityViolationException dup) {
                Optional<QuizContent> again = quizRepo.findByUserAndGoalTitleAndModuleTitle(user, goalTitle, moduleTitle);
                if (again.isPresent()) cleaned = again.get().getQuizJson();
            }

            try {
                JsonNode node = mapper.readTree(cleaned);
                return ResponseEntity.ok(Map.of("quiz", node));
            } catch (Exception parseEx) {
                return ResponseEntity.ok(Map.of("quizText", cleaned));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate quiz", "details", ex.getMessage()));
        }
    }

    @PostMapping(value = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> submit(@AuthenticationPrincipal UserDetails principal, @RequestBody QuizSubmitRequest req) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            String goalTitle = Optional.ofNullable(req.getGoalTitle()).orElse("").trim();
            String moduleTitle = Optional.ofNullable(req.getModuleTitle()).orElse("").trim();
            if (goalTitle.isEmpty() || moduleTitle.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "goalTitle and moduleTitle are required"));
            }
            Optional<QuizContent> existing = quizRepo.findByUserAndGoalTitleAndModuleTitle(user, goalTitle, moduleTitle);
            if (existing.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Quiz not found for this module"));
            }
            QuizContent qc = existing.get();

            // Compute score from stored quiz
            JsonNode quizNode = mapper.readTree(qc.getQuizJson());
            Map<Integer, Integer> correct = new HashMap<>();
            if (quizNode != null && quizNode.has("questions") && quizNode.get("questions").isArray()) {
                for (JsonNode q : quizNode.get("questions")) {
                    int qid = q.path("id").asInt();
                    int ci = q.path("correctIndex").asInt();
                    correct.put(qid, ci);
                }
            }
            int total = correct.size();
            int score = 0;
            if (req.getAnswers() != null) {
                for (var ans : req.getAnswers()) {
                    Integer ci = correct.get(ans.getQuestionId());
                    if (ci != null && ci.equals(ans.getSelectedIndex())) score++;
                }
            }
            int percent = total > 0 ? Math.round((score * 100f) / total) : 0;

            qc.setScore(score);
            qc.setCompleted(true);
            quizRepo.save(qc);

            // Mark module complete in study plan and update goal progress
            markModuleCompleteAndUpdateProgress(user, goalTitle, req.getModuleId(), moduleTitle);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "score", score,
                    "total", total,
                    "percent", percent
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to submit quiz", "details", ex.getMessage()));
        }
    }

    private void markModuleCompleteAndUpdateProgress(User user, String goalTitle, Integer moduleId, String moduleTitle) {
        try {
            Optional<StudyPlan> spOpt = studyPlanRepository.findByUserAndGoalTitle(user, goalTitle);
            if (spOpt.isPresent()) {
                StudyPlan sp = spOpt.get();
                JsonNode root = mapper.readTree(sp.getPlanJson());
                boolean changed = false;
                int minutes = 0;
                if (root != null && root.has("days") && root.get("days").isArray()) {
                    for (JsonNode day : root.get("days")) {
                        JsonNode modules = day.path("modules");
                        if (modules != null && modules.isArray()) {
                            for (JsonNode m : modules) {
                                boolean idMatches = moduleId != null && m.path("id").asInt() == moduleId;
                                boolean titleMatches = moduleTitle != null && moduleTitle.equalsIgnoreCase(m.path("title").asText(""));
                                if (idMatches || titleMatches) {
                                    if (m instanceof ObjectNode obj) {
                                        obj.put("completed", true);
                                        minutes = parseMinutes(m.path("duration").asText(""));
                                        changed = true;
                                    }
                                }
                            }
                        }
                    }
                }
                if (changed) {
                    sp.setPlanJson(mapper.writeValueAsString(root));
                    studyPlanRepository.save(sp);
                }
            }

            // Update goal progress based on completed modules over total
            Goal goal = goalRepository.findByUserAndTitle(user, goalTitle).orElse(null);
            if (goal != null) {
                Optional<StudyPlan> sp2 = studyPlanRepository.findByUserAndGoalTitle(user, goalTitle);
                int totalModules = 0;
                int done = 0;
                int minutes2 = 0;
                if (sp2.isPresent()) {
                    JsonNode root = mapper.readTree(sp2.get().getPlanJson());
                    if (root != null && root.has("days") && root.get("days").isArray()) {
                        for (JsonNode day : root.get("days")) {
                            JsonNode modules = day.path("modules");
                            if (modules != null && modules.isArray()) {
                                for (JsonNode m : modules) {
                                    totalModules++;
                                    if (m.path("completed").asBoolean(false)) done++;
                                    boolean idMatches = moduleId != null && m.path("id").asInt() == moduleId;
                                    boolean titleMatches = moduleTitle != null && moduleTitle.equalsIgnoreCase(m.path("title").asText(""));
                                    if (idMatches || titleMatches) {
                                        minutes2 = parseMinutes(m.path("duration").asText(""));
                                    }
                                }
                            }
                        }
                    }
                }
                int percent = (totalModules > 0) ? Math.round(done * 100f / totalModules) : 0;
                goal.setProgress(percent);
                goalRepository.save(goal);

                // Log completion minutes for today
                int mins = minutes2;
                LocalDate today = LocalDate.now();
                moduleCompletionLogRepository.findByUserIdAndGoalTitleAndModuleTitleAndCompletedDate(user.getId(), goalTitle, Optional.ofNullable(moduleTitle).orElse(""), today)
                        .ifPresentOrElse(existing -> {
                            if (mins > 0) { existing.setMinutes(mins); moduleCompletionLogRepository.save(existing); }
                        }, () -> {
                            ModuleCompletionLog log = new ModuleCompletionLog();
                            log.setUser(user);
                            log.setGoalTitle(goalTitle);
                            log.setModuleId(moduleId);
                            log.setModuleTitle(Optional.ofNullable(moduleTitle).orElse(""));
                            log.setMinutes(Math.max(0, mins));
                            log.setCompletedDate(today);
                            moduleCompletionLogRepository.save(log);
                        });
            }
        } catch (Exception ignore) {}
    }

    private int parseMinutes(String duration) {
        if (duration == null) return 0;
        String s = duration.toLowerCase(Locale.ROOT).replaceAll("[^0-9]", " ").trim();
        try {
            for (String part : s.split(" ")) {
                if (!part.isBlank()) return Integer.parseInt(part);
            }
        } catch (Exception ignore) {}
        return 0;
    }
}
