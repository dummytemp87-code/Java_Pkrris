package com.example.aichat.dto;

public class VideoRequest {
    private String goalTitle;
    private String moduleTitle;
    private Integer moduleId;
    private String language;

    public String getGoalTitle() { return goalTitle; }
    public void setGoalTitle(String goalTitle) { this.goalTitle = goalTitle; }
    public String getModuleTitle() { return moduleTitle; }
    public void setModuleTitle(String moduleTitle) { this.moduleTitle = moduleTitle; }
    public Integer getModuleId() { return moduleId; }
    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
