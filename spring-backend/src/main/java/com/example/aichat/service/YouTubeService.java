package com.example.aichat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class YouTubeService {

    @Value("${youtube.apiKey:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> searchBestVideo(String query) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("YouTube API key not configured (youtube.apiKey)");
        }
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/youtube/v3/search")
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("type", "video")
                .queryParam("maxResults", 1)
                .queryParam("relevanceLanguage", "en")
                .queryParam("safeSearch", "moderate")
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();
        ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Failed to search YouTube");
        }
        return resp.getBody();
    }
}
