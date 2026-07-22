package com.example.aichat.repo;

import com.example.aichat.model.Referral;
import com.example.aichat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    Optional<Referral> findByReferredUser(User referredUser);
    long countByReferrerAndStatusAndCreatedAtAfter(User referrer, String status, Instant after);
}
