package com.example.aichat.controller;

import com.example.aichat.dto.UserSettingsDto;
import com.example.aichat.model.User;
import com.example.aichat.model.UserSettings;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.repo.UserSettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {

    private final UserSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserSettingsController(UserSettingsRepository settingsRepository, UserRepository userRepository) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
    }

    private List<String> parseLanguages(UserSettings s) {
        List<String> result = new ArrayList<>();
        if (s.getLanguagesJson() != null && !s.getLanguagesJson().isBlank()) {
            try {
                JsonNode node = mapper.readTree(s.getLanguagesJson());
                if (node.isArray()) {
                    for (JsonNode n : node) {
                        String v = n.asText("").trim();
                        if (!v.isEmpty()) result.add(v);
                    }
                }
            } catch (Exception ignore) {}
        }
        if (result.isEmpty() && s.getLanguage() != null && !s.getLanguage().isBlank()) {
            result.add(s.getLanguage());
        }
        if (result.isEmpty()) {
            result.add("english");
        }
        return result;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Optional<User> userOpt = userRepository.findByEmail(principal.getUsername());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userOpt.get();
        Optional<UserSettings> settings = settingsRepository.findByUser(user);
        if (settings.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "theme", "system", "language", "english", "languages", List.of("english"),
                    "soundEnabled", true, "emailNotifications", true, "dailyReminders", true, "weeklyReport", false
            ));
        }
        UserSettings s = settings.get();
        List<String> languages = parseLanguages(s);
        return ResponseEntity.ok(Map.of(
                "theme", s.getTheme() != null ? s.getTheme() : "system",
                "language", languages.get(0),
                "languages", languages,
                "soundEnabled", s.getSoundEnabled() != null ? s.getSoundEnabled() : Boolean.TRUE,
                "emailNotifications", s.getEmailNotifications() != null ? s.getEmailNotifications() : Boolean.TRUE,
                "dailyReminders", s.getDailyReminders() != null ? s.getDailyReminders() : Boolean.TRUE,
                "weeklyReport", s.getWeeklyReport() != null ? s.getWeeklyReport() : Boolean.FALSE
        ));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> upsert(@AuthenticationPrincipal UserDetails principal, @RequestBody UserSettingsDto body) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Optional<User> userOpt = userRepository.findByEmail(principal.getUsername());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userOpt.get();
        Optional<UserSettings> existing = settingsRepository.findByUser(user);
        UserSettings s = existing.orElseGet(UserSettings::new);
        s.setUser(user);
        if (body.getTheme() != null) s.setTheme(body.getTheme());
        if (body.getLanguages() != null && !body.getLanguages().isEmpty()) {
            try {
                s.setLanguagesJson(mapper.writeValueAsString(body.getLanguages()));
                s.setLanguage(body.getLanguages().get(0));
            } catch (Exception ignore) {}
        } else if (body.getLanguage() != null) {
            s.setLanguage(body.getLanguage());
            try {
                s.setLanguagesJson(mapper.writeValueAsString(List.of(body.getLanguage())));
            } catch (Exception ignore) {}
        }
        if (body.getSoundEnabled() != null) s.setSoundEnabled(body.getSoundEnabled());
        if (body.getEmailNotifications() != null) s.setEmailNotifications(body.getEmailNotifications());
        if (body.getDailyReminders() != null) s.setDailyReminders(body.getDailyReminders());
        if (body.getWeeklyReport() != null) s.setWeeklyReport(body.getWeeklyReport());
        settingsRepository.save(s);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
