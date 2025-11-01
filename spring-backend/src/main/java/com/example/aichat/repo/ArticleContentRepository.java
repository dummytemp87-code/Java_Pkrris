package com.example.aichat.repo;

import com.example.aichat.model.ArticleContent;
import com.example.aichat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArticleContentRepository extends JpaRepository<ArticleContent, Long> {
    Optional<ArticleContent> findByUserAndGoalTitleAndModuleTitle(User user, String goalTitle, String moduleTitle);
    void deleteByUserAndGoalTitle(User user, String goalTitle);

    @Modifying
    @Query("delete from ArticleContent a where a.user.id = :userId and a.goalTitle = :goalTitle")
    void deleteByUserIdAndGoalTitle(@Param("userId") Long userId, @Param("goalTitle") String goalTitle);
}
