package com.example.aichat.repo;

import com.example.aichat.model.StudyPlan;
import com.example.aichat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findByUserAndGoalTitle(User user, String goalTitle);
    Optional<StudyPlan> findByUserEmailAndGoalTitle(String email, String goalTitle);
    void deleteByUserAndGoalTitle(User user, String goalTitle);
}
