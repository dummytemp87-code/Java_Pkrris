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
        // actually in that language -- a video in the wrong language can still rank
        // first on text relevance alone. Verify every candidate's real language
        // metadata (including for English, which used to skip this check entirely)
        // and drop any explicitly tagged as a *different* language, so a user who
        // didn't ask for e.g. Hindi never sees Hindi content just because it ranked
        // well for the same query text.
        List<Map<String, Object>> ranked = rankByLanguage(items, lang);
        if (!ranked.isEmpty()) {
            body.put("items", ranked);
            body.put("actualLanguage", lang);
            return body;
        }
        if (lang.equals("en")) {
            // Every candidate was explicitly tagged as a non-English language --
            // return the plain ranking rather than nothing.
            body.put("items", items);
            body.put("actualLanguage", "en");
            return body;
        }
        // No candidate for the requested language survived filtering. Fall back to
        // English, filtered the same way so an unrelated third language can't slip
        // through this path unverified either.
        Map<String, Object> englishBody = search(query, "en");
        List<Map<String, Object>> englishItems = extractItems(englishBody);
        List<Map<String, Object>> englishRanked = rankByLanguage(englishItems, "en");
        englishBody.put("items", !englishRanked.isEmpty() ? englishRanked : englishItems);
        englishBody.put("actualLanguage", "en");
        return englishBody;
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
     * candidate videos and returns them re-ordered and filtered: videos verified in
     * the target language come first (original relevance order preserved within that
     * group), videos with no language metadata at all follow (absence isn't proof of
     * mismatch -- most uploaders never set this field), and videos verified as a
     * *different* language are dropped entirely -- a user who asked for English
     * shouldn't see a video explicitly tagged Hindi just because it ranked well on
     * text relevance. Returns an empty list if metadata lookup fails outright or
     * every candidate is explicitly a different language, so the caller can fall
     * back to a plain, unfiltered ranking rather than surfacing nothing.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rankByLanguage(List<Map<String, Object>> items, String lang) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String vid = videoId(item);
            if (!vid.isBlank()) ids.add(vid);
        }
        if (ids.isEmpty()) return List.of();

        Map<String, String> metadataById = new java.util.HashMap<>();
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
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return List.of();
            Object rawItems = resp.getBody().get("items");
            if (!(rawItems instanceof List<?> list)) return List.of();
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> videoMap)) continue;
                Object snippetObj = videoMap.get("snippet");
                if (!(snippetObj instanceof Map<?, ?> snippet)) continue;
                Object id = videoMap.get("id");
                if (id == null) continue;
                String audioLang = str(snippet.get("defaultAudioLanguage"));
                String defaultLang = str(snippet.get("defaultLanguage"));
                String resolved = audioLang != null ? audioLang : defaultLang;
                if (resolved != null) metadataById.put(id.toString(), resolved);
            }
        } catch (RestClientException ignored) {
            return List.of();
        }

        List<Map<String, Object>> matched = new ArrayList<>();
        List<Map<String, Object>> unknown = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String vid = videoId(item);
            String metaLang = metadataById.get(vid);
            if (metaLang == null) {
                unknown.add(item);
            } else if (matches(metaLang, lang)) {
                matched.add(item);
            }
            // else: verified as a different language -- dropped.
        }
        matched.addAll(unknown);
        return matched;
    }

    private boolean matches(String metadataLang, String target) {
        return metadataLang != null && metadataLang.toLowerCase().startsWith(target.toLowerCase());
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
