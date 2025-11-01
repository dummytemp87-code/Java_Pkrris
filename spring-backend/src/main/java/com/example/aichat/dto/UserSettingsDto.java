package com.example.aichat.dto;

public class UserSettingsDto {
    private String theme;
    private String language;
    private Boolean soundEnabled;

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Boolean getSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(Boolean soundEnabled) { this.soundEnabled = soundEnabled; }
}
