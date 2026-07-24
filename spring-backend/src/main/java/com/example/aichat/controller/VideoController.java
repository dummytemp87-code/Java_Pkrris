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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
                if (existing.isPresent()) return ResponseEntity.ok(toResponse(existing.get()));
            }
            Optional<VideoContent> anyCached = videoRepo.findByUserAndGoalTitleAndModuleTitle(user, goalTitle, moduleTitle);
            if (anyCached.isPresent()) return ResponseEntity.ok(toResponse(anyCached.get()));

            // Videos already used elsewhere in this goal -- reaching this point means
            // *this* module has no cached video yet, so every cached row for this goal
            // belongs to a different module.
            List<VideoContent> usedInGoal = videoRepo.findByUserAndGoalTitle(user, goalTitle);
            Set<String> usedVideoIds = new LinkedHashSet<>();
            for (VideoContent v : usedInGoal) {
                if (v.getVideoId() != null) usedVideoIds.add(v.getVideoId());
            }

            // 1.5) Chapter reuse: if one of those already-used videos has a real YouTube
            // chapter list and one chapter's title actually matches this module's topic,
            // reuse that video pointed at that chapter instead of searching for a
            // different video (or repeating the same video with no timestamp -- showing
            // the same unscoped video for module after module). A long, single "full
            // course" video can then correctly serve many different days.
            if (!usedVideoIds.isEmpty()) {
                Map<String, YouTubeService.ChapterMatch> chapterMatches =
                        youTubeService.findChapterMatches(new ArrayList<>(usedVideoIds), moduleTitle);
                if (!chapterMatches.isEmpty()) {
                    for (VideoContent v : usedInGoal) {
                        YouTubeService.ChapterMatch match = chapterMatches.get(v.getVideoId());
                        if (match == null) continue;
                        VideoContent saved = reuseWithChapter(user, goalTitle, moduleTitle, req.getModuleId(), v, match);
                        Optional<VideoContent> conflict = saveOrReturnExisting(saved, user, goalTitle, moduleTitle, v.getLanguage());
                        return ResponseEntity.ok(toResponse(conflict.orElse(saved)));
                    }
                }
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
            // Prefer the first candidate not already used elsewhere in this goal;
            // only reuse one if every candidate returned is already in use (a repeat
            // is still better than no video at all).
            Map<?, ?> chosen = null;
            for (Object o : items) {
                Map<?, ?> candidate = (Map<?, ?>) o;
                Map<?, ?> candidateId = (Map<?, ?>) candidate.get("id");
                String candidateVideoId = candidateId != null ? (String) candidateId.get("videoId") : null;
                if (candidateVideoId != null && !usedVideoIds.contains(candidateVideoId)) {
                    chosen = candidate;
                    break;
                }
            }
            Map<?, ?> first = chosen != null ? chosen : (Map<?, ?>) items.get(0);
            Map<?, ?> id = (Map<?, ?>) first.get("id");
            Map<?, ?> snippet = (Map<?, ?>) first.get("snippet");
            String videoId = id != null ? (String) id.get("videoId") : null;
            if (videoId == null || videoId.isBlank()) {
                return ResponseEntity.status(404).body(Map.of("error", "No video found"));
            }
            String videoTitle = snippet != null ? (String) snippet.get("title") : null;
            String channelTitle = snippet != null ? (String) snippet.get("channelTitle") : null;
            String url = "https://www.youtube.com/watch?v=" + videoId;

            // Even a freshly found video might itself be chaptered (e.g. it's a broad
            // compilation) -- scope playback to the matching chapter if so.
            Integer startSeconds = null;
            Integer endSeconds = null;
            YouTubeService.ChapterMatch freshMatch = youTubeService.findChapterMatches(List.of(videoId), moduleTitle).get(videoId);
            if (freshMatch != null) {
                startSeconds = freshMatch.startSeconds;
                endSeconds = freshMatch.endSeconds;
            }

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
            saved.setStartSeconds(startSeconds);
            saved.setEndSeconds(endSeconds);

            Optional<VideoContent> conflict = saveOrReturnExisting(saved, user, goalTitle, moduleTitle, matchedLangCode);
            return ResponseEntity.ok(toResponse(conflict.orElse(saved)));
        } catch (Exception ex) {
            log.error("Failed to load video", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load video"));
        }
    }

    private VideoContent reuseWithChapter(User user, String goalTitle, String moduleTitle, Integer moduleId,
                                           VideoContent source, YouTubeService.ChapterMatch match) {
        VideoContent saved = new VideoContent();
        saved.setUser(user);
        saved.setGoalTitle(goalTitle);
        saved.setModuleId(moduleId);
        saved.setModuleTitle(moduleTitle);
        saved.setVideoId(source.getVideoId());
        saved.setVideoTitle(source.getVideoTitle());
        saved.setChannelTitle(source.getChannelTitle());
        saved.setUrl(source.getUrl());
        saved.setLanguage(source.getLanguage());
        saved.setStartSeconds(match.startSeconds);
        saved.setEndSeconds(match.endSeconds);
        return saved;
    }

    /** Saves, or on a unique-constraint race (a concurrent request already created this row), returns the existing row instead. */
    private Optional<VideoContent> saveOrReturnExisting(VideoContent saved, User user, String goalTitle, String moduleTitle, String language) {
        try {
            videoRepo.save(saved);
            return Optional.empty();
        } catch (DataIntegrityViolationException dup) {
            return videoRepo.findByUserAndGoalTitleAndModuleTitleAndLanguage(user, goalTitle, moduleTitle, language);
        }
    }

    private Map<String, Object> toResponse(VideoContent v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("videoId", v.getVideoId());
        m.put("videoTitle", v.getVideoTitle());
        m.put("channelTitle", v.getChannelTitle());
        m.put("url", v.getUrl());
        m.put("language", v.getLanguage());
        m.put("startSeconds", v.getStartSeconds());
        m.put("endSeconds", v.getEndSeconds());
        return m;
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
