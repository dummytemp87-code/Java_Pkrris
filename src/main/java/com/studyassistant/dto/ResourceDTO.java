package com.studyassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDTO {
    private Long id;
    private String title;
    private String description;
    private String resourceType;
    private String url;
    private String topic;
    private String difficultyLevel;
    private Double rating;
    private Integer viewCount;
}
