package com.studyassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime reminderTime;
    private Boolean isSent;
    private String reminderType;
    private LocalDateTime createdAt;
}
