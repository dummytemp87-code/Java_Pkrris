package com.example.aichat.repo;

import com.example.aichat.model.Subscription;
import com.example.aichat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUser(User user);
    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);
}
