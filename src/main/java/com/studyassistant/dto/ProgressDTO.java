package com.studyassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDTO {
    private Long id;
    private LocalDate studyDate;
    private Integer studyTimeMinutes;
    private Integer tasksCompleted;
    private Integer overallProgressPercentage;
    private Integer streakDays;
}
