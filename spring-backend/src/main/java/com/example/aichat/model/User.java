package com.example.aichat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email"}),
        @UniqueConstraint(columnNames = {"referral_code"})
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt hashed

    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Column(nullable = false)
    private Integer tokenVersion = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "referral_code")
    private String referralCode;

    // Not DB-NOT-NULL: adding a NOT NULL column via ddl-auto=update fails outright
    // on a table with existing rows (Postgres has no default to backfill with).
    // Existing users read as null here, which every check below treats as "not
    // verified" via Boolean.TRUE.equals(...), so this is safe either way.
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    // BCrypt-hashed OTP, matched the same way a password is -- never stored in plaintext.
    @Column(name = "verification_otp")
    private String verificationOtp;

    @Column(name = "verification_otp_expires_at")
    private Instant verificationOtpExpiresAt;

    @Column(name = "verification_otp_sent_at")
    private Instant verificationOtpSentAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(Integer tokenVersion) { this.tokenVersion = tokenVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getVerificationOtp() { return verificationOtp; }
    public void setVerificationOtp(String verificationOtp) { this.verificationOtp = verificationOtp; }

    public Instant getVerificationOtpExpiresAt() { return verificationOtpExpiresAt; }
    public void setVerificationOtpExpiresAt(Instant verificationOtpExpiresAt) { this.verificationOtpExpiresAt = verificationOtpExpiresAt; }

    public Instant getVerificationOtpSentAt() { return verificationOtpSentAt; }
    public void setVerificationOtpSentAt(Instant verificationOtpSentAt) { this.verificationOtpSentAt = verificationOtpSentAt; }
}
