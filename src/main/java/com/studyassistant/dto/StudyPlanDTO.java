package com.studyassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanDTO {
    private Long id;
    private Long goalId;
    private String dayOfWeek;
    private String topic;
    private String learningObjectives;
    private Integer durationMinutes;
    private String contentType;
    private Boolean isCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
