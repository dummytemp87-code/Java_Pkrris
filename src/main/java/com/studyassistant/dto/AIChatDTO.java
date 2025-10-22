package com.studyassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIChatDTO {
    private Long id;
    private String sessionId;
    private String userMessage;
    private String aiResponse;
    private String topic;
    private Long goalId;
    private LocalDateTime createdAt;
}
