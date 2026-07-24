package com.example.aichat.repo;

import com.example.aichat.model.StudyPlan;
import com.example.aichat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findByUserAndGoalTitle(User user, String goalTitle);
    Optional<StudyPlan> findByUserEmailAndGoalTitle(String email, String goalTitle);
    void deleteByUserAndGoalTitle(User user, String goalTitle);
    List<StudyPlan> findByUser(User user);

    @Modifying
    @Query("delete from StudyPlan s where s.user.id = :userId and s.goalTitle = :goalTitle")
    void deleteByUserIdAndGoalTitle(@Param("userId") Long userId, @Param("goalTitle") String goalTitle);
}
