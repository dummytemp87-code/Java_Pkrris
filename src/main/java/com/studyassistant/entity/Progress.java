package com.studyassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Progress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "goal_id")
    private Goal goal;

    @Column(name = "study_date")
    private LocalDate studyDate;

    @Column(name = "study_time_minutes")
    private Integer studyTimeMinutes;

    @Column(name = "tasks_completed")
    private Integer tasksCompleted;

    @Column(name = "overall_progress_percentage")
    private Integer overallProgressPercentage;

    @Column(name = "streak_days")
    private Integer streakDays = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
