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

    // Non-Latin scripts we can detect directly from the video title, with no
    // extra API call and no dependence on the uploader ever setting
    // defaultAudioLanguage/defaultLanguage (most don't). A Hindi video almost
    // always has a Devanagari title even when audio-language metadata is
    // absent, so this catches the common case the metadata check alone
    // misses entirely.
    private static final Map<String, int[][]> SCRIPT_RANGES = Map.ofEntries(
            Map.entry("hi", new int[][]{{0x0900, 0x097F}}),           // Devanagari
            Map.entry("ta", new int[][]{{0x0B80, 0x0BFF}}),           // Tamil
            Map.entry("te", new int[][]{{0x0C00, 0x0C7F}}),           // Telugu
            Map.entry("ar", new int[][]{{0x0600, 0x06FF}, {0x0750, 0x077F}}), // Arabic
            Map.entry("zh", new int[][]{{0x4E00, 0x9FFF}}),           // CJK Unified Ideographs
            Map.entry("ja", new int[][]{{0x3040, 0x30FF}}),           // Hiragana + Katakana
            Map.entry("ko", new int[][]{{0xAC00, 0xD7A3}}),           // Hangul
            Map.entry("ru", new int[][]{{0x0400, 0x04FF}})            // Cyrillic
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
     * Combines two independent language signals to re-order and filter candidates:
     * (1) real language metadata (defaultAudioLanguage/defaultLanguage) from the
     * YouTube API, and (2) the script actually used in the video's title, detected
     * locally with no extra API call. Either signal alone can miss things --
     * metadata because most uploaders never set it, script detection because it
     * only covers non-Latin-script languages and a title can be transliterated --
     * but together they catch the common cases. A video is dropped if *either*
     * signal says it's a different language; kept and promoted to the front if
     * *either* says it matches; otherwise kept as an unranked filler (a genuine
     * match elsewhere in the list always sorts ahead of it).
     */
    private List<Map<String, Object>> rankByLanguage(List<Map<String, Object>> items, String lang) {
        Map<String, String> metadataById = fetchLanguageMetadata(items);

        List<Map<String, Object>> matched = new ArrayList<>();
        List<Map<String, Object>> unknown = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String vid = videoId(item);
            String metaLang = metadataById.get(vid);
            String title = title(item);

            boolean metaMismatch = metaLang != null && !matches(metaLang, lang);
            boolean scriptMismatch = titleScriptMismatch(title, lang);
            if (metaMismatch || scriptMismatch) continue; // dropped -- a different language

            boolean metaMatch = metaLang != null && matches(metaLang, lang);
            boolean scriptMatch = titleScriptMatch(title, lang);
            if (metaMatch || scriptMatch) {
                matched.add(item);
            } else {
                unknown.add(item);
            }
        }
        matched.addAll(unknown);
        return matched;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fetchLanguageMetadata(List<Map<String, Object>> items) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String vid = videoId(item);
            if (!vid.isBlank()) ids.add(vid);
        }
        if (ids.isEmpty()) return Map.of();

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
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return Map.of();
            Object rawItems = resp.getBody().get("items");
            if (!(rawItems instanceof List<?> list)) return Map.of();
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
            // Metadata is one of two signals -- script detection below still applies
            // even when this call fails, so we don't bail out entirely here.
            return Map.of();
        }
        return metadataById;
    }

    private String title(Map<String, Object> item) {
        Object snippetObj = item.get("snippet");
        if (snippetObj instanceof Map<?, ?> snippet) {
            Object t = snippet.get("title");
            if (t != null) return t.toString();
        }
        return null;
    }

    /** True if the title visibly uses the target language's known non-Latin script. */
    private boolean titleScriptMatch(String title, String lang) {
        int[][] ranges = SCRIPT_RANGES.get(lang);
        return ranges != null && containsScript(title, ranges);
    }

    /**
     * True if the title visibly uses a *different* known script than the target
     * expects -- e.g. Devanagari in a title when the target is English (or any
     * other language that isn't Hindi), or Arabic script when the target is Hindi.
     */
    private boolean titleScriptMismatch(String title, String lang) {
        if (title == null) return false;
        for (Map.Entry<String, int[][]> e : SCRIPT_RANGES.entrySet()) {
            if (e.getKey().equals(lang)) continue;
            if (containsScript(title, e.getValue())) return true;
        }
        return false;
    }

    private boolean containsScript(String text, int[][] ranges) {
        if (text == null) return false;
        return text.codePoints().anyMatch(cp -> {
            for (int[] range : ranges) {
                if (cp >= range[0] && cp <= range[1]) return true;
            }
            return false;
        });
    }

    private boolean matches(String metadataLang, String target) {
        return metadataLang != null && metadataLang.toLowerCase().startsWith(target.toLowerCase());
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
