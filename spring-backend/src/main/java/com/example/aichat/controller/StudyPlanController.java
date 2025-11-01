package com.example.aichat.controller;

import com.example.aichat.dto.StudyPlanRequest;
import com.example.aichat.dto.ModuleCompletionRequest;
import com.example.aichat.service.OpenAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.example.aichat.model.StudyPlan;
import com.example.aichat.model.User;
import com.example.aichat.repo.StudyPlanRepository;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.model.Goal;
import com.example.aichat.repo.GoalRepository;
import com.example.aichat.model.ModuleCompletionLog;
import com.example.aichat.repo.ModuleCompletionLogRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/study-plan")
@CrossOrigin(origins = {"http://localhost:3000"})
public class StudyPlanController {

    private final OpenAIService aiService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final ModuleCompletionLogRepository moduleCompletionLogRepository;

    public StudyPlanController(OpenAIService aiService, StudyPlanRepository studyPlanRepository, UserRepository userRepository, GoalRepository goalRepository, ModuleCompletionLogRepository moduleCompletionLogRepository) {
        this.aiService = aiService;
        this.studyPlanRepository = studyPlanRepository;
        this.userRepository = userRepository;
        this.goalRepository = goalRepository;
        this.moduleCompletionLogRepository = moduleCompletionLogRepository;
    }

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generate(@AuthenticationPrincipal UserDetails principal, @RequestBody StudyPlanRequest req) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            String email = principal.getUsername();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            String goal = Optional.ofNullable(req.getGoalTitle()).orElse("General Study");
            int days = Optional.ofNullable(req.getDays()).orElse(4);
            String level = Optional.ofNullable(req.getLevel()).orElse("beginner");
            String goalKey = goal.trim();

            // Load goal details to enrich prompt (description + included topics)
            Goal goalEntity = goalRepository.findByUserAndTitle(user, goalKey).orElse(null);
            String description = goalEntity != null ? Optional.ofNullable(goalEntity.getDescription()).orElse("") : "";
            List<String> topics = new ArrayList<>();
            if (goalEntity != null && goalEntity.getTopicsJson() != null && !goalEntity.getTopicsJson().isBlank()) {
                try {
                    JsonNode tn = mapper.readTree(goalEntity.getTopicsJson());
                    if (tn.isArray()) {
                        for (JsonNode n : tn) {
                            String t = n.asText("").trim();
                            if (!t.isBlank()) topics.add(t);
                        }
                    }
                } catch (Exception ignore) {}
            }

            // 1) Return stored plan only if requested days match stored days; otherwise regenerate and update
            Optional<StudyPlan> existing = studyPlanRepository.findByUserAndGoalTitle(user, goalKey);
            if (existing.isPresent()) {
                Integer storedDays = existing.get().getDays();
                if (storedDays != null && storedDays.equals(days)) {
                    String planJson = existing.get().getPlanJson();
                    try {
                        JsonNode node = mapper.readTree(planJson);
                        return ResponseEntity.ok(Map.of("plan", node));
                    } catch (Exception parseEx) {
                        return ResponseEntity.ok(Map.of("planText", planJson));
                    }
                }
            }

            List<Map<String, String>> messages = new ArrayList<>();
            String system = "You are a study plan generator. Return STRICT JSON only (no markdown, no prose). Schema: {\n" +
                    "  \"days\": [ {\n" +
                    "    \"day\": string, \"date\": string,\n" +
                    "    \"modules\": [ { \"id\": number, \"title\": string, \"duration\": string, \"completed\": boolean, \"type\": one of [\\\"video\\\", \\\"quiz\\\"], \"description\": string } ]\n" +
                    "  } ]\n" +
                    "}. Keep durations short like '45 min'. No extra text.";
            messages.add(Map.of("role", "system", "content", system));
            StringBuilder prompt = new StringBuilder(String.format(
                    "Create a %d-day study plan for the goal: '%s'. Difficulty level: %s. Use only video and quiz modules with concise descriptions (avoid articles).",
                    days, goal, level));
            if (description != null && !description.isBlank()) {
                prompt.append("\nContext/Description: ").append(description.trim());
            }
            if (!topics.isEmpty()) {
                prompt.append("\nIncluded topics to cover (prioritize and integrate across days): ")
                        .append(String.join(", ", topics));
                prompt.append("\nEnsure modules explicitly map to these topics where appropriate.");
            }
            messages.add(Map.of("role", "user", "content", prompt.toString()));

            String content = aiService.getChatCompletion(messages);
            // sanitize fenced code blocks if present
            String cleaned = content.trim();
            if (cleaned.startsWith("```") ) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n", "");
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            }

            Map<String, Object> body = new HashMap<>();
            try {
                JsonNode node = mapper.readTree(cleaned);
                body.put("plan", node);
                // Save or update for consistency
                try {
                    StudyPlan sp = existing.orElseGet(StudyPlan::new);
                    sp.setUser(user);
                    sp.setGoalTitle(goalKey);
                    sp.setDays(days);
                    sp.setLevel(level);
                    sp.setPlanJson(mapper.writeValueAsString(node));
                    studyPlanRepository.save(sp);
                } catch (DataIntegrityViolationException dup) {
                    Optional<StudyPlan> exist2 = studyPlanRepository.findByUserAndGoalTitle(user, goalKey);
                    if (exist2.isPresent()) {
                        String pj = exist2.get().getPlanJson();
                        try {
                            JsonNode n2 = mapper.readTree(pj);
                            return ResponseEntity.ok(Map.of("plan", n2));
                        } catch (Exception ignore) {
                            return ResponseEntity.ok(Map.of("planText", pj));
                        }
                    }
                }
            } catch (Exception parseEx) {
                body.put("planText", content);
                // store raw text as fallback
                try {
                    StudyPlan sp = existing.orElseGet(StudyPlan::new);
                    sp.setUser(user);
                    sp.setGoalTitle(goalKey);
                    sp.setDays(days);
                    sp.setLevel(level);
                    sp.setPlanJson(cleaned);
                    studyPlanRepository.save(sp);
                } catch (DataIntegrityViolationException dup) {
                    Optional<StudyPlan> exist2 = studyPlanRepository.findByUserAndGoalTitle(user, goalKey);
                    if (exist2.isPresent()) {
                        String pj = exist2.get().getPlanJson();
                        try {
                            JsonNode n2 = mapper.readTree(pj);
                            return ResponseEntity.ok(Map.of("plan", n2));
                        } catch (Exception ignore) {
                            return ResponseEntity.ok(Map.of("planText", pj));
                        }
                    }
                }
            }
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Failed to generate study plan");
            err.put("details", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @PostMapping(value = "/complete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completeModule(@AuthenticationPrincipal UserDetails principal, @RequestBody ModuleCompletionRequest req) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            String goalTitle = Optional.ofNullable(req.getGoalTitle()).orElse("").trim();
            if (goalTitle.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "goalTitle is required"));

            Optional<StudyPlan> spOpt = studyPlanRepository.findByUserAndGoalTitle(user, goalTitle);
            if (spOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Study plan not found"));
            StudyPlan sp = spOpt.get();

            JsonNode root = mapper.readTree(sp.getPlanJson());
            boolean changed = false;
            if (root != null && root.has("days") && root.get("days").isArray()) {
                for (JsonNode day : root.get("days")) {
                    JsonNode modules = day.path("modules");
                    if (modules != null && modules.isArray()) {
                        for (JsonNode m : modules) {
                            boolean idMatches = req.getModuleId() != null && m.path("id").asInt() == req.getModuleId();
                            boolean titleMatches = req.getModuleTitle() != null && req.getModuleTitle().equalsIgnoreCase(m.path("title").asText(""));
                            if (idMatches || titleMatches) {
                                if (m instanceof ObjectNode obj) {
                                    obj.put("completed", Boolean.TRUE.equals(req.getCompleted()));
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

            // Recalculate goal progress
            Goal goal = goalRepository.findByUserAndTitle(user, goalTitle).orElse(null);
            int totalModules = 0;
            int done = 0;
            int minutes = 0;
            // locate the module again to parse duration
            if (root != null && root.has("days") && root.get("days").isArray()) {
                for (JsonNode day : root.get("days")) {
                    JsonNode modules = day.path("modules");
                    if (modules != null && modules.isArray()) {
                        for (JsonNode m : modules) {
                            totalModules++;
                            if (m.path("completed").asBoolean(false)) done++;
                            boolean idMatches = req.getModuleId() != null && m.path("id").asInt() == req.getModuleId();
                            boolean titleMatches = req.getModuleTitle() != null && req.getModuleTitle().equalsIgnoreCase(m.path("title").asText(""));
                            if (idMatches || titleMatches) {
                                String dur = m.path("duration").asText("");
                                minutes = parseMinutes(dur);
                            }
                        }
                    }
                }
            }
            if (root != null && root.has("days") && root.get("days").isArray()) {
                for (JsonNode day : root.get("days")) {
                    JsonNode modules = day.path("modules");
                    if (modules != null && modules.isArray()) {
                        for (JsonNode m : modules) {
                            // already counted above
                        }
                    }
                }
            }
            int percent = (totalModules > 0) ? Math.round(done * 100f / totalModules) : 0;
            if (goal != null) {
                goal.setProgress(percent);
                goalRepository.save(goal);
            }

            // Log study time for today on completion; remove when un-completing
            final int minutesFinal = minutes;
            final String moduleTitleStr = Optional.ofNullable(req.getModuleTitle()).orElse("");
            if (Boolean.TRUE.equals(req.getCompleted())) {
                final LocalDate today = LocalDate.now();
                moduleCompletionLogRepository.findByUserIdAndGoalTitleAndModuleTitleAndCompletedDate(user.getId(), goalTitle, moduleTitleStr, today)
                        .ifPresentOrElse(existing -> {
                            existing.setMinutes(minutesFinal);
                            moduleCompletionLogRepository.save(existing);
                        }, () -> {
                            ModuleCompletionLog log = new ModuleCompletionLog();
                            log.setUser(user);
                            log.setGoalTitle(goalTitle);
                            log.setModuleId(req.getModuleId());
                            log.setModuleTitle(moduleTitleStr);
                            log.setMinutes(minutesFinal);
                            log.setCompletedDate(today);
                            moduleCompletionLogRepository.save(log);
                        });
            } else {
                final LocalDate today = LocalDate.now();
                moduleCompletionLogRepository.findByUserIdAndGoalTitleAndModuleTitleAndCompletedDate(user.getId(), goalTitle, moduleTitleStr, today)
                        .ifPresent(moduleCompletionLogRepository::delete);
            }

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "goalProgress", percent,
                    "completedModules", done,
                    "totalModules", totalModules
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update module completion", "details", ex.getMessage()));
        }
    }

    @GetMapping(value = "/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> progress(@AuthenticationPrincipal UserDetails principal, @RequestParam("goalTitle") String goalTitle) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            String title = Optional.ofNullable(goalTitle).orElse("").trim();
            if (title.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "goalTitle is required"));

            Optional<StudyPlan> spOpt = studyPlanRepository.findByUserAndGoalTitle(user, title);
            int totalModules = 0;
            int done = 0;
            if (spOpt.isPresent()) {
                JsonNode root = mapper.readTree(spOpt.get().getPlanJson());
                if (root != null && root.has("days") && root.get("days").isArray()) {
                    for (JsonNode day : root.get("days")) {
                        JsonNode modules = day.path("modules");
                        if (modules != null && modules.isArray()) {
                            for (JsonNode m : modules) {
                                totalModules++;
                                if (m.path("completed").asBoolean(false)) done++;
                            }
                        }
                    }
                }
            }
            Goal goal = goalRepository.findByUserAndTitle(user, title).orElse(null);
            int percent = (totalModules > 0) ? Math.round(done * 100f / totalModules) : (goal != null ? goal.getProgress() : 0);
            return ResponseEntity.ok(Map.of(
                    "goalProgress", percent,
                    "completedModules", done,
                    "totalModules", totalModules
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch progress", "details", ex.getMessage()));
        }
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
