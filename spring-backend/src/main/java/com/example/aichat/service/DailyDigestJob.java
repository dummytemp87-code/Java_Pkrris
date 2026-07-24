package com.example.aichat.service;

import com.example.aichat.model.Goal;
import com.example.aichat.model.User;
import com.example.aichat.model.UserSettings;
import com.example.aichat.repo.GoalRepository;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.repo.UserSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Sends each opted-in user a daily email combining their study progress
 * (per-goal completion %, days left, current streak) with a nudge toward
 * whatever module is next up -- gated on UserSettings.emailNotifications AND
 * dailyReminders both being true (default true for users who never touched
 * Settings, since a UserSettings row may not exist yet).
 */
@Component
public class DailyDigestJob {

    private static final Logger log = LoggerFactory.getLogger(DailyDigestJob.class);
    private static final String APP_URL = "https://studyhub-orcin-beta.vercel.app";

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final GoalRepository goalRepository;
    private final ProgressService progressService;
    private final EmailService emailService;

    public DailyDigestJob(UserRepository userRepository,
                           UserSettingsRepository userSettingsRepository,
                           GoalRepository goalRepository,
                           ProgressService progressService,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.goalRepository = goalRepository;
        this.progressService = progressService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void sendDailyDigests() {
        List<User> users = userRepository.findAll();
        int sent = 0;
        for (User user : users) {
            try {
                if (!isOptedIn(user)) continue;
                if (sendDigestForUser(user)) sent++;
            } catch (Exception ex) {
                log.error("Failed to send daily digest to {}", user.getEmail(), ex);
            }
        }
        log.info("Daily digest job finished: sent {} of {} users", sent, users.size());
    }

    private boolean isOptedIn(User user) {
        UserSettings s = userSettingsRepository.findByUser(user).orElse(null);
        boolean emailOn = s == null || !Boolean.FALSE.equals(s.getEmailNotifications());
        boolean dailyOn = s == null || !Boolean.FALSE.equals(s.getDailyReminders());
        return emailOn && dailyOn;
    }

    private boolean sendDigestForUser(User user) {
        List<Goal> goals = goalRepository.findByUserOrderByCreatedAtDesc(user);
        if (goals.isEmpty()) return false; // nothing to report -- skip rather than send an empty email

        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(user.getName()).append(",\n\n");

        int streak = progressService.computeStreakDays(user.getId());
        if (streak > 0) {
            sb.append("You're on a ").append(streak).append("-day streak. Keep it going!\n\n");
        }

        sb.append("Your goals:\n");
        for (Goal g : goals) {
            sb.append("- ").append(g.getTitle()).append(": ").append(g.getProgress()).append("% complete");
            if (g.getTargetDate() != null) {
                long daysLeft = Math.max(0, ChronoUnit.DAYS.between(today, g.getTargetDate()));
                sb.append(", ").append(daysLeft).append(" day(s) left");
            }
            sb.append("\n");
        }

        List<Map<String, Object>> todaysTasks = progressService.computeTodaysTasks(user);
        if (!todaysTasks.isEmpty()) {
            Map<String, Object> next = todaysTasks.get(0);
            sb.append("\nUp next: \"").append(next.get("moduleTitle")).append("\" in ").append(next.get("goalTitle")).append(".\n");
        }

        sb.append("\nContinue here: ").append(APP_URL).append("\n\n- StudyHub");

        emailService.sendDigestEmail(user.getEmail(), "Your daily StudyHub update", sb.toString());
        return true;
    }
}
