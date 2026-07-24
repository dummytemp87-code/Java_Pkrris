package com.example.aichat.service;

import com.example.aichat.model.StudyPlan;
import com.example.aichat.model.User;
import com.example.aichat.repo.ModuleCompletionLogRepository;
import com.example.aichat.repo.StudyPlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Streak and "today's tasks" computation, shared between DashboardController
 * (per-request, for the authenticated user) and DailyDigestJob (scheduled,
 * for every user) -- kept in one place so the two don't drift out of sync on
 * the flexible date-parsing/fallback behavior.
 */
@Service
public class ProgressService {

    private final StudyPlanRepository studyPlanRepository;
    private final ModuleCompletionLogRepository moduleCompletionLogRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProgressService(StudyPlanRepository studyPlanRepository,
                            ModuleCompletionLogRepository moduleCompletionLogRepository) {
        this.studyPlanRepository = studyPlanRepository;
        this.moduleCompletionLogRepository = moduleCompletionLogRepository;
    }

    public int computeStreakDays(Long userId) {
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

    public List<Map<String, Object>> computeTodaysTasks(User user) {
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
                                tasks.add(taskMap(sp, m));
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
                                    tasks.add(taskMap(sp, m));
                                }
                            }
                        }
                        if (!tasks.isEmpty()) break;
                    }
                }
            } catch (Exception ignore) {}
        }
        if (tasks.size() > 12) return tasks.subList(0, 12);
        return tasks;
    }

    private Map<String, Object> taskMap(StudyPlan sp, JsonNode m) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("goalTitle", sp.getGoalTitle());
        t.put("moduleId", m.path("id").asInt());
        t.put("moduleTitle", m.path("title").asText(""));
        t.put("type", m.path("type").asText(""));
        t.put("duration", m.path("duration").asText(""));
        t.put("completed", m.path("completed").asBoolean(false));
        return t;
    }

    private LocalDate parseFlexibleDate(String s) {
        if (s == null || s.isBlank()) return null;
        List<String> patterns = List.of("yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy");
        for (String p : patterns) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignore) {}
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception ignore) {}
        return null;
    }
}
