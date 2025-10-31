package com.example.aichat.controller;

import com.example.aichat.dto.VideoRequest;
import com.example.aichat.model.User;
import com.example.aichat.model.VideoContent;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.repo.VideoContentRepository;
import com.example.aichat.service.YouTubeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = {"http://localhost:3000"})
public class VideoController {

    private final VideoContentRepository videoRepo;
    private final UserRepository userRepository;
    private final YouTubeService youTubeService;

    public VideoController(VideoContentRepository videoRepo, UserRepository userRepository, YouTubeService youTubeService) {
        this.videoRepo = videoRepo;
        this.userRepository = userRepository;
        this.youTubeService = youTubeService;
    }

    @PostMapping(value = "/content", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> content(@AuthenticationPrincipal UserDetails principal, @RequestBody VideoRequest req) {
        try {
            if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            String goalTitle = Optional.ofNullable(req.getGoalTitle()).orElse("").trim();
            String moduleTitle = Optional.ofNullable(req.getModuleTitle()).orElse("").trim();
            if (goalTitle.isEmpty() || moduleTitle.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "goalTitle and moduleTitle are required"));
            }

            Optional<VideoContent> existing = videoRepo.findByUserAndGoalTitleAndModuleTitle(user, goalTitle, moduleTitle);
            if (existing.isPresent()) {
                VideoContent v = existing.get();
                return ResponseEntity.ok(Map.of(
                        "videoId", v.getVideoId(),
                        "videoTitle", v.getVideoTitle(),
                        "channelTitle", v.getChannelTitle(),
                        "url", v.getUrl()
                ));
            }

            // Query YouTube for the best video
            String query = moduleTitle + " tutorial " + goalTitle;
            Map<String, Object> body = youTubeService.searchBestVideo(query);
            List<?> items = (List<?>) body.get("items");
            if (items == null || items.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "No video found"));
            }
            Map<?, ?> first = (Map<?, ?>) items.get(0);
            Map<?, ?> id = (Map<?, ?>) first.get("id");
            Map<?, ?> snippet = (Map<?, ?>) first.get("snippet");
            String videoId = id != null ? (String) id.get("videoId") : null;
            if (videoId == null || videoId.isBlank()) {
                return ResponseEntity.status(404).body(Map.of("error", "No video found"));
            }
            String videoTitle = snippet != null ? (String) snippet.get("title") : null;
            String channelTitle = snippet != null ? (String) snippet.get("channelTitle") : null;
            String url = "https://www.youtube.com/watch?v=" + videoId;

            VideoContent saved = new VideoContent();
            saved.setUser(user);
            saved.setGoalTitle(goalTitle);
            saved.setModuleId(req.getModuleId());
            saved.setModuleTitle(moduleTitle);
            saved.setVideoId(videoId);
            saved.setVideoTitle(videoTitle);
            saved.setChannelTitle(channelTitle);
            saved.setUrl(url);
            videoRepo.save(saved);

            return ResponseEntity.ok(Map.of(
                    "videoId", videoId,
                    "videoTitle", videoTitle,
                    "channelTitle", channelTitle,
                    "url", url
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load video", "details", ex.getMessage()));
        }
    }
}
