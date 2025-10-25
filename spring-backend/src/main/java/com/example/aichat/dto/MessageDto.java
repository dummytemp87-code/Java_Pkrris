package com.example.aichat.dto;

public class MessageDto {
    private String role; // "user" | "tutor"
    private String text;

    public MessageDto() {}

    public MessageDto(String role, String text) {
        this.role = role;
        this.text = text;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
