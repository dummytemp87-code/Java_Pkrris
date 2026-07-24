package com.example.aichat.dto;

import java.util.List;

public class VideoRequest {
    private String goalTitle;
    private String moduleTitle;
    private Integer moduleId;
    private String language;
    private List<String> languages;
    private String duration; // module's estimated study time, e.g. "30 min" -- decides the video length bucket

    public String getGoalTitle() { return goalTitle; }
    public void setGoalTitle(String goalTitle) { this.goalTitle = goalTitle; }
    public String getModuleTitle() { return moduleTitle; }
    public void setModuleTitle(String moduleTitle) { this.moduleTitle = moduleTitle; }
    public Integer getModuleId() { return moduleId; }
    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
}
