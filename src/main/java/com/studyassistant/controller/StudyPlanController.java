package com.studyassistant.controller;

import com.studyassistant.dto.StudyPlanDTO;
import com.studyassistant.service.StudyPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/study-plans")
@CrossOrigin(origins = "*")
public class StudyPlanController {
    @Autowired
    private StudyPlanService studyPlanService;

    @PostMapping("/users/{userId}")
    public ResponseEntity<StudyPlanDTO> createStudyPlan(@PathVariable Long userId, @RequestBody StudyPlanDTO planDTO) {
        try {
            StudyPlanDTO createdPlan = studyPlanService.createStudyPlan(userId, planDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPlan);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{planId}")
    public ResponseEntity<StudyPlanDTO> updateStudyPlan(@PathVariable Long planId, @RequestBody StudyPlanDTO planDTO) {
        try {
            StudyPlanDTO updatedPlan = studyPlanService.updateStudyPlan(planId, planDTO);
            return ResponseEntity.ok(updatedPlan);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{planId}/complete")
    public ResponseEntity<StudyPlanDTO> markPlanComplete(@PathVariable Long planId) {
        try {
            StudyPlanDTO completedPlan = studyPlanService.markPlanComplete(planId);
            return ResponseEntity.ok(completedPlan);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/goals/{goalId}")
    public ResponseEntity<List<StudyPlanDTO>> getGoalStudyPlans(@PathVariable Long goalId) {
        try {
            List<StudyPlanDTO> plans = studyPlanService.getGoalStudyPlans(goalId);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}/day/{dayOfWeek}")
    public ResponseEntity<List<StudyPlanDTO>> getDayStudyPlans(@PathVariable Long userId, @PathVariable String dayOfWeek) {
        try {
            List<StudyPlanDTO> plans = studyPlanService.getDayStudyPlans(userId, dayOfWeek);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deleteStudyPlan(@PathVariable Long planId) {
        try {
            studyPlanService.deleteStudyPlan(planId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
