package com.studyassistant.controller;

import com.studyassistant.dto.UserDTO;
import com.studyassistant.dto.UserSettingsDTO;
import com.studyassistant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@CrossOrigin(origins = "*")
public class SettingsController {
    @Autowired
    private UserService userService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserSettingsDTO> getUserSettings(@PathVariable Long userId) {
        try {
            UserSettingsDTO settings = userService.getUserSettings(userId);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserSettingsDTO> updateUserSettings(@PathVariable Long userId, @RequestBody UserSettingsDTO settingsDTO) {
        try {
            UserSettingsDTO updatedSettings = userService.updateUserSettings(userId, settingsDTO);
            return ResponseEntity.ok(updatedSettings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/users/{userId}/theme")
    public ResponseEntity<UserDTO> updateTheme(@PathVariable Long userId, @RequestParam String theme) {
        try {
            UserDTO user = userService.updateTheme(userId, theme);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/users/{userId}/notifications/toggle")
    public ResponseEntity<UserDTO> toggleNotifications(@PathVariable Long userId) {
        try {
            UserDTO user = userService.toggleNotifications(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/users/{userId}/daily-goal")
    public ResponseEntity<UserDTO> updateDailyGoal(@PathVariable Long userId, @RequestParam Integer minutes) {
        try {
            UserDTO user = userService.updateDailyGoal(userId, minutes);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
