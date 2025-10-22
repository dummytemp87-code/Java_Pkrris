package com.studyassistant.service;

import com.studyassistant.dto.StudyPlanDTO;
import com.studyassistant.entity.Goal;
import com.studyassistant.entity.StudyPlan;
import com.studyassistant.entity.User;
import com.studyassistant.repository.GoalRepository;
import com.studyassistant.repository.StudyPlanRepository;
import com.studyassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudyPlanService {
    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private UserRepository userRepository;

    public StudyPlanDTO createStudyPlan(Long userId, StudyPlanDTO planDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Goal goal = goalRepository.findById(planDTO.getGoalId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        StudyPlan plan = new StudyPlan();
        plan.setUser(user);
        plan.setGoal(goal);
        plan.setDayOfWeek(planDTO.getDayOfWeek());
        plan.setTopic(planDTO.getTopic());
        plan.setLearningObjectives(planDTO.getLearningObjectives());
        plan.setDurationMinutes(planDTO.getDurationMinutes());
        plan.setContentType(planDTO.getContentType());
        plan.setIsCompleted(false);

        StudyPlan savedPlan = studyPlanRepository.save(plan);
        return convertToDTO(savedPlan);
    }

    public StudyPlanDTO updateStudyPlan(Long planId, StudyPlanDTO planDTO) {
        StudyPlan plan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Study plan not found"));

        plan.setTopic(planDTO.getTopic());
        plan.setLearningObjectives(planDTO.getLearningObjectives());
        plan.setDurationMinutes(planDTO.getDurationMinutes());
        plan.setContentType(planDTO.getContentType());
        plan.setUpdatedAt(LocalDateTime.now());

        StudyPlan updatedPlan = studyPlanRepository.save(plan);
        return convertToDTO(updatedPlan);
    }

    public StudyPlanDTO markPlanComplete(Long planId) {
        StudyPlan plan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Study plan not found"));

        plan.setIsCompleted(true);
        plan.setUpdatedAt(LocalDateTime.now());

        StudyPlan updatedPlan = studyPlanRepository.save(plan);
        return convertToDTO(updatedPlan);
    }

    public List<StudyPlanDTO> getGoalStudyPlans(Long goalId) {
        return studyPlanRepository.findByGoalId(goalId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudyPlanDTO> getDayStudyPlans(Long userId, String dayOfWeek) {
        return studyPlanRepository.findByUserIdAndDayOfWeek(userId, dayOfWeek).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void deleteStudyPlan(Long planId) {
        studyPlanRepository.deleteById(planId);
    }

    private StudyPlanDTO convertToDTO(StudyPlan plan) {
        return new StudyPlanDTO(
                plan.getId(),
                plan.getGoal().getId(),
                plan.getDayOfWeek(),
                plan.getTopic(),
                plan.getLearningObjectives(),
                plan.getDurationMinutes(),
                plan.getContentType(),
                plan.getIsCompleted(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
