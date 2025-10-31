package com.example.aichat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "article_content", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "goal_title", "module_title"})
})
public class ArticleContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "goal_title", nullable = false)
    private String goalTitle;

    @Column(name = "module_id")
    private Integer moduleId; // optional; uniqueness by title

    @Column(name = "module_title", nullable = false)
    private String moduleTitle;

    @Lob
    @Column(name = "content_md", nullable = false, columnDefinition = "LONGTEXT")
    private String contentMarkdown;

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
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
