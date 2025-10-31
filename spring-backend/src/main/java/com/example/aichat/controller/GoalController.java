package com.example.aichat.controller;

import com.example.aichat.dto.CreateGoalRequest;
import com.example.aichat.dto.GoalDto;
import com.example.aichat.model.Goal;
import com.example.aichat.model.User;
import com.example.aichat.repo.GoalRepository;
import com.example.aichat.repo.StudyPlanRepository;
import com.example.aichat.repo.ArticleContentRepository;
import com.example.aichat.repo.VideoContentRepository;
import com.example.aichat.repo.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin(origins = {"http://localhost:3000"})
public class GoalController {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final ArticleContentRepository articleContentRepository;
    private final VideoContentRepository videoContentRepository;

    public GoalController(GoalRepository goalRepository, UserRepository userRepository, StudyPlanRepository studyPlanRepository, ArticleContentRepository articleContentRepository, VideoContentRepository videoContentRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.articleContentRepository = articleContentRepository;
        this.videoContentRepository = videoContentRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        List<Goal> goals = goalRepository.findByUserOrderByCreatedAtDesc(user);
        LocalDate today = LocalDate.now();
        List<GoalDto> dtos = goals.stream().map(g -> new GoalDto(
                g.getId(),
                g.getTitle(),
                g.getProgress(),
                g.getTargetDate() != null ? (int) Math.max(0, ChronoUnit.DAYS.between(today, g.getTargetDate())) : 0
        )).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails principal, @RequestBody CreateGoalRequest req) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            String title = Optional.ofNullable(req.getTitle()).orElse("").trim();
            if (title.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));

            Goal g = new Goal();
            g.setUser(user);
            g.setTitle(title);
            g.setDescription(req.getDescription());
            if (req.getTargetDate() != null && !req.getTargetDate().isBlank()) {
                g.setTargetDate(LocalDate.parse(req.getTargetDate()));
            }
            g.setProgress(0);
            Goal saved = goalRepository.save(g);

            LocalDate today = LocalDate.now();
            int daysLeft = saved.getTargetDate() != null ? (int) Math.max(0, ChronoUnit.DAYS.between(today, saved.getTargetDate())) : 0;
            return ResponseEntity.ok(new GoalDto(saved.getId(), saved.getTitle(), saved.getProgress(), daysLeft));
        } catch (DataIntegrityViolationException dup) {
            return ResponseEntity.status(409).body(Map.of("error", "Goal with this title already exists"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create goal", "details", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            Goal g = goalRepository.findById(id).orElse(null);
            if (g == null || !g.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404).body(Map.of("error", "Goal not found"));
            }
            // delete related study plan for this goal if any
            studyPlanRepository.deleteByUserAndGoalTitle(user, g.getTitle());
            // delete related article content for this goal if any
            articleContentRepository.deleteByUserAndGoalTitle(user, g.getTitle());
            // delete related video content for this goal if any
            videoContentRepository.deleteByUserAndGoalTitle(user, g.getTitle());
            goalRepository.delete(g);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete goal", "details", ex.getMessage()));
        }
    }
}
