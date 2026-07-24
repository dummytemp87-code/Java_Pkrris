package com.example.aichat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "referrals", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"referred_user_id"})
})
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referrer_id")
    private User referrer;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referred_user_id")
    private User referredUser;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING | REWARDED

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant rewardedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getReferrer() { return referrer; }
    public void setReferrer(User referrer) { this.referrer = referrer; }

    public User getReferredUser() { return referredUser; }
    public void setReferredUser(User referredUser) { this.referredUser = referredUser; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getRewardedAt() { return rewardedAt; }
    public void setRewardedAt(Instant rewardedAt) { this.rewardedAt = rewardedAt; }
}
