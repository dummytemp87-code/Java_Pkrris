package com.example.aichat.repo;

import com.example.aichat.model.User;
import com.example.aichat.model.VideoContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VideoContentRepository extends JpaRepository<VideoContent, Long> {
    Optional<VideoContent> findByUserAndGoalTitleAndModuleTitleAndLanguage(User user, String goalTitle, String moduleTitle, String language);
    Optional<VideoContent> findByUserAndGoalTitleAndModuleTitle(User user, String goalTitle, String moduleTitle);
    void deleteByUserAndGoalTitle(User user, String goalTitle);

    @Modifying
    @Query("delete from VideoContent v where v.user.id = :userId and v.goalTitle = :goalTitle")
    void deleteByUserIdAndGoalTitle(@Param("userId") Long userId, @Param("goalTitle") String goalTitle);
}
