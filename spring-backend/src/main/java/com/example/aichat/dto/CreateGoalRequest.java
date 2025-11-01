package com.example.aichat.dto;

public class CreateGoalRequest {
    private String title;
    private String description;
    private String targetDate; // ISO yyyy-MM-dd
    private java.util.List<String> topics;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
    public java.util.List<String> getTopics() { return topics; }
    public void setTopics(java.util.List<String> topics) { this.topics = topics; }
}
