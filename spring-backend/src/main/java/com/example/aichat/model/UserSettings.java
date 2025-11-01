package com.example.aichat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
})
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "theme")
    private String theme;

    @Column(name = "language")
    private String language;

    @Column(name = "sound_enabled")
    private Boolean soundEnabled;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Boolean getSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(Boolean soundEnabled) { this.soundEnabled = soundEnabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
