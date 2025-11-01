package com.example.aichat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URI;
import java.util.Map;

@Service
public class YouTubeService {

    @Value("${youtube.apiKey:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public YouTubeService() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(4000);
        f.setReadTimeout(6000);
        this.restTemplate = new RestTemplate(f);
    }

    public Map<String, Object> searchBestVideo(String query, String languageCode) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("YouTube API key not configured (youtube.apiKey)");
        }
        RestClientException lastEx = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                URI uri = UriComponentsBuilder
                        .fromHttpUrl("https://www.googleapis.com/youtube/v3/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", query)
                        .queryParam("type", "video")
                        .queryParam("maxResults", 3)
                        .queryParam("order", "relevance")
                        .queryParam("videoEmbeddable", "true")
                        .queryParam("relevanceLanguage", languageCode != null && !languageCode.isBlank() ? languageCode : "en")
                        .queryParam("safeSearch", "moderate")
                        .queryParam("key", apiKey)
                        .build()
                        .encode()
                        .toUri();
                ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    return resp.getBody();
                }
            } catch (RestClientException ex) {
                lastEx = ex;
            }
            try { Thread.sleep(250L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        throw new RuntimeException("Failed to search YouTube", lastEx);
    }
}
