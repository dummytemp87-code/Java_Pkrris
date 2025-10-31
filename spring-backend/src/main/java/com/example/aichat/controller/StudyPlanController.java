package com.example.aichat.controller;

import com.example.aichat.dto.StudyPlanRequest;
import com.example.aichat.service.OpenAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aichat.model.StudyPlan;
import com.example.aichat.model.User;
import com.example.aichat.repo.StudyPlanRepository;
import com.example.aichat.repo.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

@RestController
@RequestMapping("/api/study-plan")
@CrossOrigin(origins = {"http://localhost:3000"})
public class StudyPlanController {

    private final OpenAIService aiService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;

    public StudyPlanController(OpenAIService aiService, StudyPlanRepository studyPlanRepository, UserRepository userRepository) {
        this.aiService = aiService;
        this.studyPlanRepository = studyPlanRepository;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generate(@AuthenticationPrincipal UserDetails principal, @RequestBody StudyPlanRequest req) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            String email = principal.getUsername();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            String goal = Optional.ofNullable(req.getGoalTitle()).orElse("General Study");
            int days = Optional.ofNullable(req.getDays()).orElse(4);
            String level = Optional.ofNullable(req.getLevel()).orElse("beginner");
            String goalKey = goal.trim();

            // 1) Return stored plan if exists
            Optional<StudyPlan> existing = studyPlanRepository.findByUserAndGoalTitle(user, goalKey);
            if (existing.isPresent()) {
                String planJson = existing.get().getPlanJson();
                try {
                    JsonNode node = mapper.readTree(planJson);
                    return ResponseEntity.ok(Map.of("plan", node));
                } catch (Exception parseEx) {
                    return ResponseEntity.ok(Map.of("planText", planJson));
                }
            }

            List<Map<String, String>> messages = new ArrayList<>();
            String system = "You are a study plan generator. Return STRICT JSON only (no markdown, no prose). Schema: {\n" +
                    "  \"days\": [ {\n" +
                    "    \"day\": string, \"date\": string,\n" +
                    "    \"modules\": [ { \"id\": number, \"title\": string, \"duration\": string, \"completed\": boolean, \"type\": one of [\\\"video\\\",\\\"article\\\",\\\"quiz\\\"], \"description\": string } ]\n" +
                    "  } ]\n" +
                    "}. Keep durations short like '45 min'. No extra text.";
            messages.add(Map.of("role", "system", "content", system));

            String prompt = String.format("Create a %d-day study plan for the goal: '%s'. Difficulty level: %s. Ensure a balanced mix of video, article, and quiz modules with concise descriptions.", days, goal, level);
            messages.add(Map.of("role", "user", "content", prompt));

            String content = aiService.getChatCompletion(messages);
            // sanitize fenced code blocks if present
            String cleaned = content.trim();
            if (cleaned.startsWith("```") ) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n", "");
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            }

            Map<String, Object> body = new HashMap<>();
            try {
                JsonNode node = mapper.readTree(cleaned);
                body.put("plan", node);
                // Save normalized JSON for consistency
                try {
                    StudyPlan sp = new StudyPlan();
                    sp.setUser(user);
                    sp.setGoalTitle(goalKey);
                    sp.setDays(days);
                    sp.setLevel(level);
                    sp.setPlanJson(mapper.writeValueAsString(node));
                    studyPlanRepository.save(sp);
                } catch (DataIntegrityViolationException dup) {
                    Optional<StudyPlan> exist2 = studyPlanRepository.findByUserAndGoalTitle(user, goalKey);
                    if (exist2.isPresent()) {
                        String pj = exist2.get().getPlanJson();
                        try {
                            JsonNode n2 = mapper.readTree(pj);
                            return ResponseEntity.ok(Map.of("plan", n2));
                        } catch (Exception ignore) {
                            return ResponseEntity.ok(Map.of("planText", pj));
                        }
                    }
                }
            } catch (Exception parseEx) {
                body.put("planText", content);
                // store raw text as fallback
                try {
                    StudyPlan sp = new StudyPlan();
                    sp.setUser(user);
                    sp.setGoalTitle(goalKey);
                    sp.setDays(days);
                    sp.setLevel(level);
                    sp.setPlanJson(cleaned);
                    studyPlanRepository.save(sp);
                } catch (DataIntegrityViolationException dup) {
                    Optional<StudyPlan> exist2 = studyPlanRepository.findByUserAndGoalTitle(user, goalKey);
                    if (exist2.isPresent()) {
                        String pj = exist2.get().getPlanJson();
                        try {
                            JsonNode n2 = mapper.readTree(pj);
                            return ResponseEntity.ok(Map.of("plan", n2));
                        } catch (Exception ignore) {
                            return ResponseEntity.ok(Map.of("planText", pj));
                        }
                    }
                }
            }
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Failed to generate study plan");
            err.put("details", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }
}
