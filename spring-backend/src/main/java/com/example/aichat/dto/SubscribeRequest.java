package com.example.aichat.dto;

public class SubscribeRequest {
    private String plan; // "starter" | "pro"

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
}
