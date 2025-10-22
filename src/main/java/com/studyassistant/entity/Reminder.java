package com.studyassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reminder_time")
    private LocalDateTime reminderTime;

    @Column(name = "is_sent")
    private Boolean isSent = false;

    @Column(name = "reminder_type")
    private String reminderType; // TASK, STUDY, GOAL

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
