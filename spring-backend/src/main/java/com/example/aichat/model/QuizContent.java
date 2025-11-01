package com.example.aichat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "quiz_content", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "goal_title", "module_title"})
})
public class QuizContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "goal_title", nullable = false)
    private String goalTitle;

    @Column(name = "module_id")
    private Integer moduleId;

    @Column(name = "module_title", nullable = false)
    private String moduleTitle;

    @Lob
    @Column(name = "quiz_json", nullable = false, columnDefinition = "LONGTEXT")
    private String quizJson;

    @Column(name = "score")
    private Integer score;

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getGoalTitle() { return goalTitle; }
    public void setGoalTitle(String goalTitle) { this.goalTitle = goalTitle; }
    public Integer getModuleId() { return moduleId; }
    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }
    public String getModuleTitle() { return moduleTitle; }
    public void setModuleTitle(String moduleTitle) { this.moduleTitle = moduleTitle; }
    public String getQuizJson() { return quizJson; }
    public void setQuizJson(String quizJson) { this.quizJson = quizJson; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
