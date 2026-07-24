package com.example.aichat.dto;

public class GoalDto {
    private Long id;
    private String title;
    private int progress;
    private int daysLeft;

    public GoalDto() {}

    public GoalDto(Long id, String title, int progress, int daysLeft) {
        this.id = id;
        this.title = title;
        this.progress = progress;
        this.daysLeft = daysLeft;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public int getDaysLeft() { return daysLeft; }
    public void setDaysLeft(int daysLeft) { this.daysLeft = daysLeft; }
}
