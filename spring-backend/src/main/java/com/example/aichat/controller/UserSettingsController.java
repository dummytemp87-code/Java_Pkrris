package com.example.aichat.controller;

import com.example.aichat.dto.UserSettingsDto;
import com.example.aichat.model.User;
import com.example.aichat.model.UserSettings;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.repo.UserSettingsRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = {"http://localhost:3000"})
public class UserSettingsController {

    private final UserSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    public UserSettingsController(UserSettingsRepository settingsRepository, UserRepository userRepository) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Optional<User> userOpt = userRepository.findByEmail(principal.getUsername());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userOpt.get();
        Optional<UserSettings> settings = settingsRepository.findByUser(user);
        if (settings.isEmpty()) {
            return ResponseEntity.ok(Map.of("theme", "light", "language", "english", "soundEnabled", true));
        }
        UserSettings s = settings.get();
        return ResponseEntity.ok(Map.of(
                "theme", s.getTheme() != null ? s.getTheme() : "light",
                "language", s.getLanguage() != null ? s.getLanguage() : "english",
                "soundEnabled", s.getSoundEnabled() != null ? s.getSoundEnabled() : Boolean.TRUE
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
        if (body.getLanguage() != null) s.setLanguage(body.getLanguage());
        if (body.getSoundEnabled() != null) s.setSoundEnabled(body.getSoundEnabled());
        settingsRepository.save(s);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
