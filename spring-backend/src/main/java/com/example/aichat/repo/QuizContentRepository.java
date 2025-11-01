package com.example.aichat.repo;

import com.example.aichat.model.QuizContent;
import com.example.aichat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface QuizContentRepository extends JpaRepository<QuizContent, Long> {
    Optional<QuizContent> findByUserAndGoalTitleAndModuleTitle(User user, String goalTitle, String moduleTitle);

    List<QuizContent> findByUser(User user);

    @Modifying
    @Query("delete from QuizContent q where q.user.id = :userId and q.goalTitle = :goalTitle")
    void deleteByUserIdAndGoalTitle(@Param("userId") Long userId, @Param("goalTitle") String goalTitle);
}
