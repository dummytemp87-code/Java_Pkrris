package com.example.aichat.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "goals", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "title"})
})
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Lob
    @Column(name = "topics_json", columnDefinition = "LONGTEXT")
    private String topicsJson;

    private LocalDate targetDate;

    @Column(nullable = false)
    private int progress = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTopicsJson() { return topicsJson; }
    public void setTopicsJson(String topicsJson) { this.topicsJson = topicsJson; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
