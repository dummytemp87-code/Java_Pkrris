package com.example.aichat.dto;

public class ChatReplyDto {
    private String reply;

    public ChatReplyDto() {}

    public ChatReplyDto(String reply) {
        this.reply = reply;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
