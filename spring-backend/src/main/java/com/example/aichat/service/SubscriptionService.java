package com.example.aichat.service;

import com.example.aichat.model.Subscription;
import com.example.aichat.model.User;
import com.example.aichat.repo.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public boolean isEntitled(User user) {
        Subscription sub = subscriptionRepository.findByUser(user).orElse(null);
        if (sub == null) return false;
        if ("ACTIVE".equals(sub.getStatus())) return true;
        return "TRIALING".equals(sub.getStatus())
                && sub.getTrialEndsAt() != null
                && sub.getTrialEndsAt().isAfter(Instant.now());
    }
}
