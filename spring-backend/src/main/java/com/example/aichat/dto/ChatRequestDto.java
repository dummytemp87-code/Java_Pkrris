package com.example.aichat.dto;

import java.util.List;

public class ChatRequestDto {
    private List<MessageDto> messages;
    private String systemPrompt;

    public ChatRequestDto() {}

    public List<MessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageDto> messages) {
        this.messages = messages;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
