package com.studyassistant.controller;

import com.studyassistant.dto.GoalDTO;
import com.studyassistant.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/goals")
@CrossOrigin(origins = "*")
public class GoalController {
    @Autowired
    private GoalService goalService;

    @PostMapping("/users/{userId}")
    public ResponseEntity<GoalDTO> createGoal(@PathVariable Long userId, @RequestBody GoalDTO goalDTO) {
        try {
            GoalDTO createdGoal = goalService.createGoal(userId, goalDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<GoalDTO> updateGoal(@PathVariable Long goalId, @RequestBody GoalDTO goalDTO) {
        try {
            GoalDTO updatedGoal = goalService.updateGoal(goalId, goalDTO);
            return ResponseEntity.ok(updatedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{goalId}/progress")
    public ResponseEntity<GoalDTO> updateProgress(@PathVariable Long goalId, @RequestParam Integer progressPercentage) {
        try {
            GoalDTO updatedGoal = goalService.updateProgress(goalId, progressPercentage);
            return ResponseEntity.ok(updatedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<GoalDTO>> getUserGoals(@PathVariable Long userId) {
        try {
            List<GoalDTO> goals = goalService.getUserGoals(userId);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}/active")
    public ResponseEntity<List<GoalDTO>> getActiveGoals(@PathVariable Long userId) {
        try {
            List<GoalDTO> goals = goalService.getActiveGoals(userId);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long goalId) {
        try {
            goalService.deleteGoal(goalId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
