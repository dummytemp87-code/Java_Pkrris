package com.example.aichat.dto;

import java.util.List;

public class UserSettingsDto {
    private String theme;
    private String language;
    private List<String> languages;
    private Boolean soundEnabled;
    private Boolean emailNotifications;
    private Boolean dailyReminders;
    private Boolean weeklyReport;

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }
    public Boolean getSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(Boolean soundEnabled) { this.soundEnabled = soundEnabled; }
    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public Boolean getDailyReminders() { return dailyReminders; }
    public void setDailyReminders(Boolean dailyReminders) { this.dailyReminders = dailyReminders; }
    public Boolean getWeeklyReport() { return weeklyReport; }
    public void setWeeklyReport(Boolean weeklyReport) { this.weeklyReport = weeklyReport; }
}
