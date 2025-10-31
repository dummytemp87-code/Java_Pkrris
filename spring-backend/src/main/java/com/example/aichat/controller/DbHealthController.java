package com.example.aichat.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/db")
public class DbHealthController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/health")
    @CrossOrigin(origins = {"http://localhost:3000"})
    public ResponseEntity<?> health() {
        Map<String, Object> result = new HashMap<>();
        try {
            Object one = entityManager.createNativeQuery("SELECT 1").getSingleResult();
            result.put("status", "up");
            result.put("db", one);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            result.put("status", "down");
            result.put("error", ex.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
