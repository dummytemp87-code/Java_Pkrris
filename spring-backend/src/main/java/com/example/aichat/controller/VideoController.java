package com.example.aichat.controller;

import com.example.aichat.dto.VideoRequest;
import com.example.aichat.model.User;
import com.example.aichat.model.VideoContent;
import com.example.aichat.repo.UserRepository;
import com.example.aichat.repo.VideoContentRepository;
import com.example.aichat.service.YouTubeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

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

            // Ordered preference list: explicit "languages" wins, falls back to legacy single "language".
            List<String> rawLanguages = req.getLanguages();
            if (rawLanguages == null || rawLanguages.isEmpty()) {
                rawLanguages = List.of(Optional.ofNullable(req.getLanguage()).orElse("english"));
            }
            List<String> langCodes = new ArrayList<>();
            for (String lang : rawLanguages) {
                String code = toLangCode(lang);
                if (!langCodes.contains(code)) langCodes.add(code);
            }
            if (langCodes.isEmpty()) langCodes.add("en");

            // 1) Cache: check each preferred language in order before falling back to any cached language.
            for (String langCode : langCodes) {
                Optional<VideoContent> existing = videoRepo.findByUserAndGoalTitleAndModuleTitleAndLanguage(user, goalTitle, moduleTitle, langCode);
                if (existing.isPresent()) {
                    VideoContent v = existing.get();
                    return ResponseEntity.ok(Map.of(
                            "videoId", v.getVideoId(),
                            "videoTitle", v.getVideoTitle(),
                            "channelTitle", v.getChannelTitle(),
                            "url", v.getUrl(),
                            "language", v.getLanguage()
                    ));
                }
            }
            Optional<VideoContent> anyCached = videoRepo.findByUserAndGoalTitleAndModuleTitle(user, goalTitle, moduleTitle);
            if (anyCached.isPresent()) {
                VideoContent v = anyCached.get();
                return ResponseEntity.ok(Map.of(
                        "videoId", v.getVideoId(),
                        "videoTitle", v.getVideoTitle(),
                        "channelTitle", v.getChannelTitle(),
                        "url", v.getUrl(),
                        "language", v.getLanguage()
                ));
            }

            // 2) Fresh search: try each preferred language in order, falling back to the next if nothing found.
            String[] queries = new String[] {
                    moduleTitle + " tutorial " + goalTitle,
                    moduleTitle + " tutorial",
                    moduleTitle
            };
            List<?> items = null;
            String matchedLangCode = null;
            for (String langCode : langCodes) {
                for (String q : queries) {
                    Map<String, Object> body = youTubeService.searchBestVideo(q, langCode, req.getDuration());
                    items = (List<?>) body.get("items");
                    if (items != null && !items.isEmpty()) {
                        Object actual = body.get("actualLanguage");
                        matchedLangCode = actual != null ? actual.toString() : langCode;
                        break;
                    }
                }
                if (items != null && !items.isEmpty()) {
                    break;
                }
            }
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

            try {
                VideoContent saved = new VideoContent();
                saved.setUser(user);
                saved.setGoalTitle(goalTitle);
                saved.setModuleId(req.getModuleId());
                saved.setModuleTitle(moduleTitle);
                saved.setVideoId(videoId);
                saved.setVideoTitle(videoTitle);
                saved.setChannelTitle(channelTitle);
                saved.setUrl(url);
                saved.setLanguage(matchedLangCode);
                videoRepo.save(saved);
            } catch (DataIntegrityViolationException dup) {
                Optional<VideoContent> existing2 = videoRepo.findByUserAndGoalTitleAndModuleTitleAndLanguage(user, goalTitle, moduleTitle, matchedLangCode);
                if (existing2.isPresent()) {
                    VideoContent v = existing2.get();
                    return ResponseEntity.ok(Map.of(
                            "videoId", v.getVideoId(),
                            "videoTitle", v.getVideoTitle(),
                            "channelTitle", v.getChannelTitle(),
                            "url", v.getUrl(),
                            "language", v.getLanguage()
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "videoId", videoId,
                    "videoTitle", videoTitle,
                    "channelTitle", channelTitle,
                    "url", url,
                    "language", matchedLangCode
            ));
        } catch (Exception ex) {
            log.error("Failed to load video", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load video"));
        }
    }

    private String toLangCode(String language) {
        String l = Optional.ofNullable(language).orElse("english").trim().toLowerCase();
        switch (l) {
            case "en": case "english": return "en";
            case "es": case "spanish": return "es";
            case "fr": case "french": return "fr";
            case "de": case "german": return "de";
            default: return l.length() == 2 ? l : "en";
        }
    }
}
