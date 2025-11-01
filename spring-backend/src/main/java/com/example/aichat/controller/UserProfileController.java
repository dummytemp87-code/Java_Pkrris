package com.example.aichat.controller;

import com.example.aichat.dto.UserProfileUpdateRequest;
import com.example.aichat.dto.AuthResponse;
import com.example.aichat.model.User;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/profile")
@CrossOrigin(origins = {"http://localhost:3000"})
public class UserProfileController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public UserProfileController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        return ResponseEntity.ok(Map.of(
                "name", user.getName(),
                "email", user.getEmail()
        ));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody UserProfileUpdateRequest req) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        boolean emailChanged = !user.getEmail().equalsIgnoreCase(req.getEmail());
        if (emailChanged && userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already in use"));
        }
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        if (emailChanged) {
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        }
        userRepository.save(user);
        String token = jwtService.generateToken(user.getEmail(), user.getTokenVersion() != null ? user.getTokenVersion() : 0);
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail(), user.getRole()));
    }
}
