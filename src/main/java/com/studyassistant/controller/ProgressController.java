package com.studyassistant.controller;

import com.studyassistant.dto.ProgressDTO;
import com.studyassistant.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/progress")
@CrossOrigin(origins = "*")
public class ProgressController {
    @Autowired
    private ProgressService progressService;

    @PostMapping("/users/{userId}")
    public ResponseEntity<ProgressDTO> recordProgress(@PathVariable Long userId, @RequestBody ProgressDTO progressDTO) {
        try {
            ProgressDTO recordedProgress = progressService.recordProgress(userId, progressDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(recordedProgress);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ProgressDTO>> getUserProgress(@PathVariable Long userId) {
        try {
            List<ProgressDTO> progress = progressService.getUserProgress(userId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}/range")
    public ResponseEntity<List<ProgressDTO>> getProgressRange(
            @PathVariable Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        try {
            List<ProgressDTO> progress = progressService.getProgressRange(userId, startDate, endDate);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
