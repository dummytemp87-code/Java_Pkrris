package com.example.aichat.controller;

import com.example.aichat.model.User;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.repo.ModuleCompletionLogRepository;
import com.example.aichat.service.ProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final UserRepository userRepository;
    private final ModuleCompletionLogRepository moduleCompletionLogRepository;
    private final ProgressService progressService;

    public DashboardController(UserRepository userRepository,
                               ModuleCompletionLogRepository moduleCompletionLogRepository,
                               ProgressService progressService) {
        this.userRepository = userRepository;
        this.moduleCompletionLogRepository = moduleCompletionLogRepository;
        this.progressService = progressService;
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
            int streak = progressService.computeStreakDays(user.getId());

            List<Map<String, Object>> todaysTasks = progressService.computeTodaysTasks(user);

            Map<String, Object> body = new HashMap<>();
            body.put("studyMinutesToday", studyMinutesToday);
            body.put("tasksCompletedToday", tasksCompletedToday);
            body.put("streakDays", streak);
            body.put("todaysTasks", todaysTasks);
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            log.error("Failed to load dashboard summary", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load dashboard summary"));
        }
    }

}
