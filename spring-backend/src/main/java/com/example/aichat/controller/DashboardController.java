package com.example.aichat.controller;

import com.example.aichat.model.StudyPlan;
import com.example.aichat.model.User;
import com.example.aichat.repo.StudyPlanRepository;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.repo.ModuleCompletionLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"http://localhost:3000"})
public class DashboardController {

    private final UserRepository userRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final ModuleCompletionLogRepository moduleCompletionLogRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public DashboardController(UserRepository userRepository,
                               StudyPlanRepository studyPlanRepository,
                               ModuleCompletionLogRepository moduleCompletionLogRepository) {
        this.userRepository = userRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.moduleCompletionLogRepository = moduleCompletionLogRepository;
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> summary(@AuthenticationPrincipal UserDetails principal) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            LocalDate today = LocalDate.now();
            Integer studyMinutesToday = Optional.ofNullable(moduleCompletionLogRepository.sumMinutesByUserAndDate(user.getId(), today)).orElse(0);
            Integer tasksCompletedToday = Optional.ofNullable(moduleCompletionLogRepository.countByUserAndDate(user.getId(), today)).orElse(0);
            int streak = computeStreakDays(user.getId());

            List<Map<String, Object>> todaysTasks = computeTodaysTasks(user);

            Map<String, Object> body = new HashMap<>();
            body.put("studyMinutesToday", studyMinutesToday);
            body.put("tasksCompletedToday", tasksCompletedToday);
            body.put("streakDays", streak);
            body.put("todaysTasks", todaysTasks);
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load dashboard summary", "details", ex.getMessage()));
        }
    }

    private int computeStreakDays(Long userId) {
        int streak = 0;
        LocalDate day = LocalDate.now();
        for (int i = 0; i < 60; i++) { // check up to 60 days back
            Integer minutes = moduleCompletionLogRepository.sumMinutesByUserAndDate(userId, day);
            if (minutes != null && minutes > 0) streak++;
            else break;
            day = day.minusDays(1);
        }
        return streak;
    }

    private List<Map<String, Object>> computeTodaysTasks(User user) {
        LocalDate today = LocalDate.now();
        List<StudyPlan> plans = studyPlanRepository.findByUser(user);
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (StudyPlan sp : plans) {
            try {
                JsonNode root = mapper.readTree(sp.getPlanJson());
                if (root == null || !root.has("days") || !root.get("days").isArray()) continue;
                boolean anyAdded = false;
                for (JsonNode day : root.get("days")) {
                    String dateStr = day.path("date").asText("").trim();
                    LocalDate parsed = parseFlexibleDate(dateStr);
                    if (parsed != null && parsed.equals(today)) {
                        JsonNode mods = day.path("modules");
                        if (mods != null && mods.isArray()) {
                            for (JsonNode m : mods) {
                                Map<String, Object> t = new LinkedHashMap<>();
                                t.put("goalTitle", sp.getGoalTitle());
                                t.put("moduleId", m.path("id").asInt());
                                t.put("moduleTitle", m.path("title").asText(""));
                                t.put("type", m.path("type").asText(""));
                                t.put("duration", m.path("duration").asText(""));
                                t.put("completed", m.path("completed").asBoolean(false));
                                tasks.add(t);
                                anyAdded = true;
                            }
                        }
                    }
                }
                // Fallback: add earliest day with uncompleted modules if no exact date match
                if (!anyAdded) {
                    for (JsonNode day : root.get("days")) {
                        JsonNode mods = day.path("modules");
                        if (mods != null && mods.isArray()) {
                            for (JsonNode m : mods) {
                                if (!m.path("completed").asBoolean(false)) {
                                    Map<String, Object> t = new LinkedHashMap<>();
                                    t.put("goalTitle", sp.getGoalTitle());
                                    t.put("moduleId", m.path("id").asInt());
                                    t.put("moduleTitle", m.path("title").asText(""));
                                    t.put("type", m.path("type").asText(""));
                                    t.put("duration", m.path("duration").asText(""));
                                    t.put("completed", false);
                                    tasks.add(t);
                                }
                            }
                        }
                        if (!tasks.isEmpty()) break;
                    }
                }
            } catch (Exception ignore) {}
        }
        // Cap tasks to a reasonable number
        if (tasks.size() > 12) return tasks.subList(0, 12);
        return tasks;
    }

    private LocalDate parseFlexibleDate(String s) {
        if (s == null || s.isBlank()) return null;
        List<String> patterns = List.of("yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy");
        for (String p : patterns) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignore) {}
        }
        try { // try default ISO
            return LocalDate.parse(s);
        } catch (Exception ignore) {}
        return null;
    }
}
