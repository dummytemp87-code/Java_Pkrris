package com.example.aichat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from:}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String name, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        if (from != null && !from.isBlank()) msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Your StudyHub verification code");
        msg.setText("Hi " + name + ",\n\n"
                + "Your StudyHub verification code is: " + otp + "\n\n"
                + "This code expires in 10 minutes. If you didn't request this, you can ignore this email.\n\n"
                + "- StudyHub");
        mailSender.send(msg);
        log.info("Sent verification OTP email to {}", to);
    }

    public void sendDigestEmail(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        if (from != null && !from.isBlank()) msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}
