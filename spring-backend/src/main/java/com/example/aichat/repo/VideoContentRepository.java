package com.example.aichat.repo;

import com.example.aichat.model.User;
import com.example.aichat.model.VideoContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoContentRepository extends JpaRepository<VideoContent, Long> {
    Optional<VideoContent> findByUserAndGoalTitleAndModuleTitle(User user, String goalTitle, String moduleTitle);
    void deleteByUserAndGoalTitle(User user, String goalTitle);
}
