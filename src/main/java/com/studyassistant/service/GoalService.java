package com.studyassistant.service;

import com.studyassistant.dto.GoalDTO;
import com.studyassistant.entity.Goal;
import com.studyassistant.entity.User;
import com.studyassistant.repository.GoalRepository;
import com.studyassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoalService {
    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private UserRepository userRepository;

    public GoalDTO createGoal(Long userId, GoalDTO goalDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTitle(goalDTO.getTitle());
        goal.setDescription(goalDTO.getDescription());
        goal.setSubjectArea(goalDTO.getSubjectArea());
        goal.setTargetCompletionDate(goalDTO.getTargetCompletionDate());
        goal.setSyllabusUrl(goalDTO.getSyllabusUrl());
        goal.setStatus("ACTIVE");
        goal.setProgressPercentage(0);

        Goal savedGoal = goalRepository.save(goal);
        return convertToDTO(savedGoal);
    }

    public GoalDTO updateGoal(Long goalId, GoalDTO goalDTO) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        goal.setTitle(goalDTO.getTitle());
        goal.setDescription(goalDTO.getDescription());
        goal.setSubjectArea(goalDTO.getSubjectArea());
        goal.setTargetCompletionDate(goalDTO.getTargetCompletionDate());
        goal.setProgressPercentage(goalDTO.getProgressPercentage());
        goal.setStatus(goalDTO.getStatus());
        goal.setUpdatedAt(LocalDateTime.now());

        Goal updatedGoal = goalRepository.save(goal);
        return convertToDTO(updatedGoal);
    }

    public GoalDTO updateProgress(Long goalId, Integer progressPercentage) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        goal.setProgressPercentage(progressPercentage);
        goal.setUpdatedAt(LocalDateTime.now());

        Goal updatedGoal = goalRepository.save(goal);
        return convertToDTO(updatedGoal);
    }

    public List<GoalDTO> getUserGoals(Long userId) {
        return goalRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<GoalDTO> getActiveGoals(Long userId) {
        return goalRepository.findByUserIdAndStatus(userId, "ACTIVE").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void deleteGoal(Long goalId) {
        goalRepository.deleteById(goalId);
    }

    private GoalDTO convertToDTO(Goal goal) {
        return new GoalDTO(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getSubjectArea(),
                goal.getTargetCompletionDate(),
                goal.getProgressPercentage(),
                goal.getStatus(),
                goal.getSyllabusUrl(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
