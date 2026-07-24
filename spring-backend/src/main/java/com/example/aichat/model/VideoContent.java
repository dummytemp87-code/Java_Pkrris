package com.example.aichat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "video_content", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "goal_title", "module_title", "language"})
})
public class VideoContent {

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

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "video_title")
    private String videoTitle;

    @Column(name = "channel_title")
    private String channelTitle;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "language")
    private String language;

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
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }
    public String getChannelTitle() { return channelTitle; }
    public void setChannelTitle(String channelTitle) { this.channelTitle = channelTitle; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
