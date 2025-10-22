package com.studyassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String themePreference;
    private Boolean notificationEnabled;
    private Integer dailyGoalMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
