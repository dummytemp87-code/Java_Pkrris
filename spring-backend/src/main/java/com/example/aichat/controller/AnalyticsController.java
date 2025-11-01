package com.example.aichat.controller;

import com.example.aichat.model.ModuleCompletionLog;
import com.example.aichat.model.StudyPlan;
import com.example.aichat.model.User;
import com.example.aichat.model.QuizContent;
import com.example.aichat.repo.ModuleCompletionLogRepository;
import com.example.aichat.repo.StudyPlanRepository;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.repo.QuizContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = {"http://localhost:3000"})
public class AnalyticsController {

    private final UserRepository userRepository;
    private final ModuleCompletionLogRepository logRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final QuizContentRepository quizContentRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalyticsController(UserRepository userRepository,
                               ModuleCompletionLogRepository logRepository,
                               StudyPlanRepository studyPlanRepository,
                               QuizContentRepository quizContentRepository) {
        this.userRepository = userRepository;
        this.logRepository = logRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.quizContentRepository = quizContentRepository;
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> summary(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        LocalDate today = LocalDate.now();
        LocalDate start7 = today.minusDays(6);
        List<ModuleCompletionLog> weekLogs = logRepository.findByUserIdAndCompletedDateBetween(user.getId(), start7, today);
        Map<LocalDate, Integer> minutesByDay = new HashMap<>();
        int totalMinutesWeek = 0;
        for (ModuleCompletionLog l : weekLogs) {
            int mins = Optional.ofNullable(l.getMinutes()).orElse(0);
            minutesByDay.merge(l.getCompletedDate(), mins, Integer::sum);
            totalMinutesWeek += mins;
        }
        List<Map<String, Object>> studyTime = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start7.plusDays(i);
            int mins = minutesByDay.getOrDefault(d, 0);
            double hours = Math.round((mins / 60.0) * 10.0) / 10.0;
            String dayLabel = d.getDayOfWeek().name().substring(0, 3).charAt(0) + d.getDayOfWeek().name().substring(1, 3).toLowerCase(Locale.ROOT);
            studyTime.add(Map.of("day", dayLabel, "hours", hours));
        }

        List<StudyPlan> plans = studyPlanRepository.findByUser(user);
        int modulesTotal = 0;
        int modulesCompleted = 0;
        Map<String, Integer> typeCounts = new HashMap<>();
        for (StudyPlan sp : plans) {
            try {
                JsonNode root = mapper.readTree(sp.getPlanJson());
                if (root != null && root.has("days") && root.get("days").isArray()) {
                    for (JsonNode day : root.get("days")) {
                        JsonNode modules = day.path("modules");
                        if (modules != null && modules.isArray()) {
                            for (JsonNode m : modules) {
                                modulesTotal++;
                                if (m.path("completed").asBoolean(false)) modulesCompleted++;
                                String type = m.path("type").asText("");
                                if (!type.isBlank()) typeCounts.merge(type.toLowerCase(Locale.ROOT), 1, Integer::sum);
                            }
                        }
                    }
                }
            } catch (Exception ignore) {}
        }

        List<Map<String, Object>> contentType = new ArrayList<>();
        int typeSum = typeCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (typeSum > 0) {
            for (var e : typeCounts.entrySet()) {
                int value = (int) Math.round(e.getValue() * 100.0 / typeSum);
                String name = switch (e.getKey()) { case "video" -> "Videos"; case "quiz" -> "Quizzes"; case "article" -> "Articles"; default -> e.getKey(); };
                contentType.add(Map.of("name", name, "value", value));
            }
        }

        List<QuizContent> quizzes = quizContentRepository.findByUser(user);
        int scored = 0;
        int scoreSumPercent = 0;
        for (QuizContent qc : quizzes) {
            try {
                if (qc.getScore() == null) continue;
                JsonNode qn = mapper.readTree(qc.getQuizJson());
                int totalQ = 0;
                if (qn != null && qn.has("questions") && qn.get("questions").isArray()) {
                    totalQ = qn.get("questions").size();
                }
                if (totalQ > 0) {
                    int percent = Math.round(qc.getScore() * 100f / totalQ);
                    scoreSumPercent += percent;
                    scored++;
                }
            } catch (Exception ignore) {}
        }
        int averageQuizScore = scored > 0 ? Math.round(scoreSumPercent * 1f / scored) : 0;

        int currentStreakDays = 0;
        for (int i = 0; i < 60; i++) {
            LocalDate d = today.minusDays(i);
            Integer c = logRepository.countByUserAndDate(user.getId(), d);
            if (c != null && c > 0) currentStreakDays++;
            else break;
        }

        LocalDate startOfThisWeek = today.minusDays(today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate startOfOldestWeek = startOfThisWeek.minusWeeks(3);
        List<ModuleCompletionLog> monthLogs = logRepository.findByUserIdAndCompletedDateBetween(user.getId(), startOfOldestWeek, today);
        Map<String, LocalDate> earliestCompletion = new HashMap<>();
        for (ModuleCompletionLog l : monthLogs) {
            String key = l.getGoalTitle() + "|" + Optional.ofNullable(l.getModuleTitle()).orElse("");
            LocalDate ex = earliestCompletion.get(key);
            if (ex == null || l.getCompletedDate().isBefore(ex)) earliestCompletion.put(key, l.getCompletedDate());
        }
        List<Map<String, Object>> progress = new ArrayList<>();
        for (int w = 3; w >= 0; w--) {
            LocalDate weekEnd = startOfThisWeek.minusWeeks(3 - w).plusDays(6);
            int completedToWeek = 0;
            for (LocalDate date : earliestCompletion.values()) {
                if (!date.isAfter(weekEnd)) completedToWeek++;
            }
            int percent = modulesTotal > 0 ? Math.round(completedToWeek * 100f / modulesTotal) : 0;
            progress.add(Map.of("week", "Week " + (4 - w), "progress", percent));
        }

        return ResponseEntity.ok(Map.of(
                "totalStudyMinutesThisWeek", totalMinutesWeek,
                "modulesCompleted", modulesCompleted,
                "modulesTotal", modulesTotal,
                "averageQuizScore", averageQuizScore,
                "currentStreakDays", currentStreakDays,
                "studyTime", studyTime,
                "progress", progress,
                "contentType", contentType
        ));
    }
}
