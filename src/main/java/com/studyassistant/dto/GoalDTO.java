package com.studyassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalDTO {
    private Long id;
    private String title;
    private String description;
    private String subjectArea;
    private LocalDateTime targetCompletionDate;
    private Integer progressPercentage;
    private String status;
    private String syllabusUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
