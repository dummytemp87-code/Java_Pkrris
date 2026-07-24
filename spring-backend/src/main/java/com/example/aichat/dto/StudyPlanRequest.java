package com.example.aichat.dto;

public class StudyPlanRequest {
    private String goalTitle;
    private Integer days;
    private String level; // optional: beginner/intermediate/advanced

    public String getGoalTitle() { return goalTitle; }
    public void setGoalTitle(String goalTitle) { this.goalTitle = goalTitle; }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}
