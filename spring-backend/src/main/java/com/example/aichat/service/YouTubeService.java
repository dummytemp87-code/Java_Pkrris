package com.example.aichat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class YouTubeService {

    // Only non-English entries: appending "in English" to a query tends to hurt
    // results more than it helps, since most English videos don't state it explicitly.
    private static final Map<String, String> LANGUAGE_NAMES = Map.ofEntries(
            Map.entry("es", "Spanish"),
            Map.entry("fr", "French"),
            Map.entry("de", "German"),
            Map.entry("hi", "Hindi"),
            Map.entry("ta", "Tamil"),
            Map.entry("te", "Telugu"),
            Map.entry("pt", "Portuguese"),
            Map.entry("it", "Italian"),
            Map.entry("zh", "Chinese"),
            Map.entry("ja", "Japanese"),
            Map.entry("ko", "Korean"),
            Map.entry("ar", "Arabic"),
            Map.entry("ru", "Russian")
    );

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
        String lang = (languageCode != null && !languageCode.isBlank()) ? languageCode : "en";
        String langName = LANGUAGE_NAMES.get(lang);
        String searchQuery = langName != null ? query + " in " + langName : query;

        Map<String, Object> body = search(searchQuery, lang);
        List<Map<String, Object>> items = extractItems(body);

        // relevanceLanguage only biases ranking, it doesn't guarantee the result is
        // actually in that language. Verify against each video's real language metadata
        // and promote the first genuine match to the top, without discarding the rest.
        if (langName != null) {
            String matchedId = items.isEmpty() ? null : findLanguageMatch(items, lang);
            if (matchedId != null) {
                items.sort((a, b) -> videoId(a).equals(matchedId) ? -1 : videoId(b).equals(matchedId) ? 1 : 0);
                body.put("items", items);
                body.put("actualLanguage", lang);
                return body;
            }
            // Either the language-enriched query returned nothing, or no candidate
            // genuinely matched. Default to a plain English search rather than silently
            // returning an off-language video (or nothing) as if it were a real match.
            Map<String, Object> englishBody = search(query, "en");
            List<Map<String, Object>> englishItems = extractItems(englishBody);
            englishBody.put("items", englishItems);
            englishBody.put("actualLanguage", "en");
            return englishBody;
        }

        body.put("items", items);
        body.put("actualLanguage", lang);
        return body;
    }

    private Map<String, Object> search(String query, String lang) {
        RestClientException lastEx = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                URI uri = UriComponentsBuilder
                        .fromHttpUrl("https://www.googleapis.com/youtube/v3/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", query)
                        .queryParam("type", "video")
                        .queryParam("maxResults", 5)
                        .queryParam("order", "relevance")
                        .queryParam("videoEmbeddable", "true")
                        .queryParam("relevanceLanguage", lang)
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Map<String, Object> body) {
        Object raw = body.get("items");
        List<Map<String, Object>> items = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) items.add((Map<String, Object>) m);
            }
        }
        return items;
    }

    private String videoId(Map<String, Object> item) {
        Object id = item.get("id");
        if (id instanceof Map<?, ?> idMap) {
            Object v = idMap.get("videoId");
            if (v != null) return v.toString();
        }
        return "";
    }

    /**
     * Looks up real language metadata (defaultAudioLanguage/defaultLanguage) for the
     * candidate videos and returns the ID of the first one that actually matches, or
     * null if none do (many uploaders don't set this field at all, so absence isn't
     * treated as a mismatch — we just don't reorder in that case).
     */
    @SuppressWarnings("unchecked")
    private String findLanguageMatch(List<Map<String, Object>> items, String lang) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String vid = videoId(item);
            if (!vid.isBlank()) ids.add(vid);
        }
        if (ids.isEmpty()) return null;

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://www.googleapis.com/youtube/v3/videos")
                    .queryParam("part", "snippet")
                    .queryParam("id", String.join(",", ids))
                    .queryParam("key", apiKey)
                    .build()
                    .encode()
                    .toUri();
            ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;
            Object rawItems = resp.getBody().get("items");
            if (!(rawItems instanceof List<?> list)) return null;
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> videoMap)) continue;
                Object snippetObj = videoMap.get("snippet");
                if (!(snippetObj instanceof Map<?, ?> snippet)) continue;
                String audioLang = str(snippet.get("defaultAudioLanguage"));
                String defaultLang = str(snippet.get("defaultLanguage"));
                if (matches(audioLang, lang) || matches(defaultLang, lang)) {
                    Object id = videoMap.get("id");
                    return id != null ? id.toString() : null;
                }
            }
        } catch (RestClientException ignored) {
            // verification is a best-effort enhancement; fall back to the original ranking
        }
        return null;
    }

    private boolean matches(String metadataLang, String target) {
        return metadataLang != null && metadataLang.toLowerCase().startsWith(target.toLowerCase());
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
