package com.studyassistant.controller;

import com.studyassistant.dto.ReminderDTO;
import com.studyassistant.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/reminders")
@CrossOrigin(origins = "*")
public class ReminderController {
    @Autowired
    private ReminderService reminderService;

    @PostMapping("/users/{userId}")
    public ResponseEntity<ReminderDTO> createReminder(@PathVariable Long userId, @RequestBody ReminderDTO reminderDTO) {
        try {
            ReminderDTO createdReminder = reminderService.createReminder(userId, reminderDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdReminder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{reminderId}")
    public ResponseEntity<ReminderDTO> updateReminder(@PathVariable Long reminderId, @RequestBody ReminderDTO reminderDTO) {
        try {
            ReminderDTO updatedReminder = reminderService.updateReminder(reminderId, reminderDTO);
            return ResponseEntity.ok(updatedReminder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ReminderDTO>> getUserReminders(@PathVariable Long userId) {
        try {
            List<ReminderDTO> reminders = reminderService.getUserReminders(userId);
            return ResponseEntity.ok(reminders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}/pending")
    public ResponseEntity<List<ReminderDTO>> getPendingReminders(@PathVariable Long userId) {
        try {
            List<ReminderDTO> reminders = reminderService.getPendingReminders(userId);
            return ResponseEntity.ok(reminders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{reminderId}/sent")
    public ResponseEntity<ReminderDTO> markReminderSent(@PathVariable Long reminderId) {
        try {
            ReminderDTO reminder = reminderService.markReminderSent(reminderId);
            return ResponseEntity.ok(reminder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long reminderId) {
        try {
            reminderService.deleteReminder(reminderId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
