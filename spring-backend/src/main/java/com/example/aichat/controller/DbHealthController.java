package com.example.aichat.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/db")
public class DbHealthController {

    private static final Logger log = LoggerFactory.getLogger(DbHealthController.class);

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> result = new HashMap<>();
        try {
            Object one = entityManager.createNativeQuery("SELECT 1").getSingleResult();
            result.put("status", "up");
            result.put("db", one);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            log.error("DB health check failed", ex);
            result.put("status", "down");
            return ResponseEntity.status(500).body(result);
        }
    }
}
