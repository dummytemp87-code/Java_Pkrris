package com.example.aichat.repo;

import com.example.aichat.model.ArticleContent;
import com.example.aichat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleContentRepository extends JpaRepository<ArticleContent, Long> {
    Optional<ArticleContent> findByUserAndGoalTitleAndModuleTitle(User user, String goalTitle, String moduleTitle);
    void deleteByUserAndGoalTitle(User user, String goalTitle);
}
