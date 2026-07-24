package com.example.aichat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Absolute floor regardless of the requested search bucket -- catches Shorts
    // (YouTube's own cap is 3 minutes) even if one slips past videoDuration
    // bucketing, which isn't perfectly reliable on YouTube's end.
    private static final int MIN_DURATION_SECONDS = 240;

    // A module's estimated study time (e.g. "30 min", "1 hour") decides which
    // YouTube videoDuration bucket to search: "medium" (4-20 min) for a normal
    // focused lesson, "long" (20+ min) for denser/longer modules.
    private static final int LONG_BUCKET_THRESHOLD_MINUTES = 20;
    private static final Pattern HOURS_PATTERN = Pattern.compile("(\\d+)\\s*hour");
    private static final Pattern MINUTES_PATTERN = Pattern.compile("(\\d+)\\s*min");

    // Matches YouTube-style chapter lines in a video description. Tolerates a
    // short decorative prefix before the timestamp (bullet/emoji, common on
    // channels like freeCodeCamp: "⌨️ (0:00) Introduction") and optional
    // parentheses around the timestamp itself, in addition to the plain
    // "0:00 Introduction" / "1:23:45 - Kinematics basics" forms.
    private static final Pattern CHAPTER_LINE = Pattern.compile(
            "(?m)^[^\\d\\n]{0,6}\\(?(?:(\\d+):)?(\\d{1,2}):(\\d{2})\\)?\\s*[-:]?\\s*(.+)$"
    );
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "of", "to", "in", "and", "for", "on", "with", "is", "are", "part", "chapter"
    );
    // Words too generic to count as a real topic match on their own -- two
    // unrelated chapters both called "Introduction" shouldn't be treated as
    // the same topic just because they share that one word.
    private static final Set<String> GENERIC_WORDS = Set.of(
            "introduction", "overview", "basics", "basic", "fundamentals", "intro", "review", "summary", "recap"
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

    public Map<String, Object> searchBestVideo(String query, String languageCode, String moduleDuration) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("YouTube API key not configured (youtube.apiKey)");
        }
        String lang = (languageCode != null && !languageCode.isBlank()) ? languageCode : "en";
        String langName = LANGUAGE_NAMES.get(lang);
        String searchQuery = langName != null ? query + " in " + langName : query;
        String bucket = pickDurationBucket(moduleDuration);

        SearchResult primary = searchWithFallback(searchQuery, lang, bucket);
        if (!primary.ranked.isEmpty()) {
            primary.body.put("items", primary.ranked);
            primary.body.put("actualLanguage", lang);
            return primary.body;
        }
        if (lang.equals("en")) {
            // Every candidate was explicitly tagged as a non-English language (or too
            // short) -- return the plain ranking rather than nothing.
            primary.body.put("items", primary.items);
            primary.body.put("actualLanguage", "en");
            return primary.body;
        }
        // No candidate for the requested language survived filtering. Fall back to
        // English, filtered the same way so an unrelated third language can't slip
        // through this path unverified either.
        SearchResult english = searchWithFallback(query, "en", bucket);
        english.body.put("items", !english.ranked.isEmpty() ? english.ranked : english.items);
        english.body.put("actualLanguage", "en");
        return english.body;
    }

    /**
     * Searches the given duration bucket, then -- only if nothing survives
     * filtering -- relaxes one step at a time ("long" -> "medium" -> unconstrained)
     * rather than jumping straight to unconstrained. A niche topic may genuinely
     * have no long-form video, but a medium-length one is still closer to what
     * was asked for than whatever ranks highest with no duration constraint at all.
     */
    private SearchResult searchWithFallback(String query, String lang, String bucket) {
        List<String> attempts = "long".equals(bucket)
                ? java.util.Arrays.asList("long", "medium", null)
                : java.util.Arrays.asList(bucket, null);

        Map<String, Object> body = null;
        List<Map<String, Object>> items = List.of();
        List<Map<String, Object>> ranked = List.of();
        for (String attempt : attempts) {
            body = search(query, lang, attempt);
            items = extractItems(body);
            ranked = rankAndFilter(items, lang);
            if (!ranked.isEmpty()) break;
        }
        return new SearchResult(body, items, ranked);
    }

    private static final class SearchResult {
        final Map<String, Object> body;
        final List<Map<String, Object>> items;
        final List<Map<String, Object>> ranked;
        SearchResult(Map<String, Object> body, List<Map<String, Object>> items, List<Map<String, Object>> ranked) {
            this.body = body; this.items = items; this.ranked = ranked;
        }
    }

    private String pickDurationBucket(String moduleDuration) {
        Integer minutes = parseMinutes(moduleDuration);
        if (minutes == null) return "medium";
        return minutes > LONG_BUCKET_THRESHOLD_MINUTES ? "long" : "medium";
    }

    /** Parses strings like "30 min", "45 minutes", "1 hour", or a range like "15-40 min" (uses the upper bound). */
    private Integer parseMinutes(String s) {
        if (s == null || s.isBlank()) return null;
        String lower = s.toLowerCase();
        Matcher hourMatch = HOURS_PATTERN.matcher(lower);
        if (hourMatch.find()) return Integer.parseInt(hourMatch.group(1)) * 60;
        Matcher m = MINUTES_PATTERN.matcher(lower);
        Integer last = null;
        while (m.find()) last = Integer.parseInt(m.group(1));
        return last;
    }

    private Map<String, Object> search(String query, String lang, String videoDurationBucket) {
        RestClientException lastEx = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                UriComponentsBuilder builder = UriComponentsBuilder
                        .fromHttpUrl("https://www.googleapis.com/youtube/v3/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", query)
                        .queryParam("type", "video")
                        // 10 rather than 5 -- a reputable channel's video often isn't
                        // YouTube's literal #1-5 text-relevance hit for a specific module
                        // title, so a narrower pool was excluding it before it ever got a
                        // chance to be considered by the view-count ordering below.
                        .queryParam("maxResults", 10)
                        .queryParam("order", "relevance")
                        .queryParam("videoEmbeddable", "true")
                        .queryParam("relevanceLanguage", lang)
                        .queryParam("safeSearch", "moderate")
                        .queryParam("key", apiKey);
                if (videoDurationBucket != null && !videoDurationBucket.isBlank()) {
                    builder = builder.queryParam("videoDuration", videoDurationBucket);
                }
                URI uri = builder.build().encode().toUri();
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
     * Combines four independent signals to filter and re-order candidates:
     * (1) exact runtime from videos.list contentDetails -- a hard floor that
     * drops anything under MIN_DURATION_SECONDS regardless of language, since a
     * Short is unsuitable no matter what language it's in; (2) real language
     * metadata (defaultAudioLanguage/defaultLanguage); (3) the script actually
     * used in the video's title, detected locally; (4) view count, as a tiebreak
     * among whatever survives (1)-(3) -- plain relevance-search order otherwise
     * just reflects keyword-matching, not quality, so a well-known channel with
     * far more views than a keyword-stuffed also-ran wasn't reliably winning.
     * Either language signal alone can miss things -- metadata because most
     * uploaders never set it, script detection because it only covers
     * non-Latin-script languages -- but together they catch the common cases. A
     * video is dropped if it's too short, or if either language signal says
     * it's a different language; kept if either language signal says it
     * matches (most-viewed first), otherwise kept as unranked filler
     * (most-viewed first within that group too).
     */
    private List<Map<String, Object>> rankAndFilter(List<Map<String, Object>> items, String lang) {
        Map<String, VideoMeta> metaById = fetchVideoMetadata(items);

        List<Map<String, Object>> matched = new ArrayList<>();
        List<Map<String, Object>> unknown = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String vid = videoId(item);
            VideoMeta meta = metaById.get(vid);
            String title = title(item);

            if (meta != null && meta.durationSeconds != null && meta.durationSeconds < MIN_DURATION_SECONDS) {
                continue; // too short -- a Short or thin clip, drop regardless of language match
            }

            String metaLang = meta != null ? meta.language : null;
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
        java.util.Comparator<Map<String, Object>> byViewsDesc = java.util.Comparator.comparingLong(
                (Map<String, Object> item) -> {
                    VideoMeta meta = metaById.get(videoId(item));
                    return meta != null && meta.viewCount != null ? meta.viewCount : 0L;
                }
        ).reversed();
        matched.sort(byViewsDesc);
        unknown.sort(byViewsDesc);
        matched.addAll(unknown);
        return matched;
    }

    private static final class VideoMeta {
        final String language;
        final Integer durationSeconds;
        final Long viewCount;
        VideoMeta(String language, Integer durationSeconds, Long viewCount) {
            this.language = language; this.durationSeconds = durationSeconds; this.viewCount = viewCount;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, VideoMeta> fetchVideoMetadata(List<Map<String, Object>> items) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String vid = videoId(item);
            if (!vid.isBlank()) ids.add(vid);
        }
        if (ids.isEmpty()) return Map.of();

        Map<String, VideoMeta> metaById = new java.util.HashMap<>();
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://www.googleapis.com/youtube/v3/videos")
                    .queryParam("part", "snippet,contentDetails,statistics")
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
                Object id = videoMap.get("id");
                if (id == null) continue;

                String resolvedLang = null;
                Object snippetObj = videoMap.get("snippet");
                if (snippetObj instanceof Map<?, ?> snippet) {
                    String audioLang = str(snippet.get("defaultAudioLanguage"));
                    String defaultLang = str(snippet.get("defaultLanguage"));
                    resolvedLang = audioLang != null ? audioLang : defaultLang;
                }

                Integer seconds = null;
                Object contentDetailsObj = videoMap.get("contentDetails");
                if (contentDetailsObj instanceof Map<?, ?> contentDetails) {
                    String iso = str(contentDetails.get("duration"));
                    if (iso != null) {
                        try { seconds = (int) Duration.parse(iso).getSeconds(); } catch (Exception ignore) {}
                    }
                }

                Long viewCount = null;
                Object statisticsObj = videoMap.get("statistics");
                if (statisticsObj instanceof Map<?, ?> statistics) {
                    String vc = str(statistics.get("viewCount"));
                    if (vc != null) {
                        try { viewCount = Long.parseLong(vc); } catch (NumberFormatException ignore) {}
                    }
                }

                metaById.put(id.toString(), new VideoMeta(resolvedLang, seconds, viewCount));
            }
        } catch (RestClientException ignored) {
            // Metadata is one signal among several -- script detection and the
            // search-time duration bucket still apply even when this call fails.
            return Map.of();
        }
        return metaById;
    }

    public static final class ChapterMatch {
        public final int startSeconds;
        public final Integer endSeconds; // null = play to the end of the video
        public ChapterMatch(int startSeconds, Integer endSeconds) {
            this.startSeconds = startSeconds; this.endSeconds = endSeconds;
        }
    }

    private static final class Chapter {
        final int seconds;
        final String title;
        Chapter(int seconds, String title) { this.seconds = seconds; this.title = title; }
    }

    /**
     * For each of the given video IDs that has a real YouTube chapter list and
     * a chapter whose title shares a specific (non-generic) word with
     * moduleTitle, returns the matching chapter's start/end seconds. Lets one
     * long course video serve several different modules by pointing each at a
     * different, actually-relevant chapter instead of either repeating the
     * same unscoped video or picking a worse one just to be different.
     */
    public Map<String, ChapterMatch> findChapterMatches(List<String> videoIds, String moduleTitle) {
        if (videoIds.isEmpty() || moduleTitle == null || moduleTitle.isBlank()) return Map.of();
        Map<String, String> descriptions = fetchDescriptions(videoIds);
        Map<String, ChapterMatch> result = new HashMap<>();
        for (Map.Entry<String, String> e : descriptions.entrySet()) {
            List<Chapter> chapters = parseChapters(e.getValue());
            if (chapters.isEmpty()) continue;
            bestChapterMatch(chapters, moduleTitle).ifPresent(c ->
                    result.put(e.getKey(), new ChapterMatch(c.seconds, chapterEndSeconds(chapters, c))));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fetchDescriptions(List<String> videoIds) {
        Map<String, String> result = new HashMap<>();
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://www.googleapis.com/youtube/v3/videos")
                    .queryParam("part", "snippet")
                    .queryParam("id", String.join(",", videoIds))
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
                Object id = videoMap.get("id");
                Object snippetObj = videoMap.get("snippet");
                if (id == null || !(snippetObj instanceof Map<?, ?> snippet)) continue;
                String description = str(snippet.get("description"));
                if (description != null) result.put(id.toString(), description);
            }
        } catch (RestClientException ignored) {
            return Map.of();
        }
        return result;
    }

    /** YouTube itself requires >=3 timestamp lines starting at/near 0:00 to treat a description as real chapters. */
    private List<Chapter> parseChapters(String description) {
        if (description == null) return List.of();
        List<Chapter> chapters = new ArrayList<>();
        Matcher m = CHAPTER_LINE.matcher(description);
        while (m.find()) {
            int hours = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            int minutes = Integer.parseInt(m.group(2));
            int seconds = Integer.parseInt(m.group(3));
            String title = m.group(4).trim();
            if (title.isEmpty()) continue;
            chapters.add(new Chapter(hours * 3600 + minutes * 60 + seconds, title));
        }
        chapters.sort(java.util.Comparator.comparingInt(c -> c.seconds));
        if (chapters.size() < 3 || chapters.get(0).seconds > 5) return List.of();
        return chapters;
    }

    private Optional<Chapter> bestChapterMatch(List<Chapter> chapters, String moduleTitle) {
        Set<String> moduleWords = significantWords(moduleTitle);
        if (moduleWords.isEmpty()) return Optional.empty();

        Chapter best = null;
        int bestScore = 0;
        for (Chapter c : chapters) {
            Set<String> overlap = new HashSet<>(moduleWords);
            overlap.retainAll(significantWords(c.title));
            boolean hasSpecificOverlap = overlap.stream().anyMatch(w -> !GENERIC_WORDS.contains(w));
            if (!hasSpecificOverlap) continue;
            if (overlap.size() > bestScore) { bestScore = overlap.size(); best = c; }
        }
        return Optional.ofNullable(best);
    }

    private Integer chapterEndSeconds(List<Chapter> chapters, Chapter matched) {
        int idx = chapters.indexOf(matched);
        if (idx < 0 || idx == chapters.size() - 1) return null; // last chapter -- play to the end
        return chapters.get(idx + 1).seconds;
    }

    private Set<String> significantWords(String s) {
        Set<String> words = new HashSet<>();
        for (String w : s.toLowerCase().split("[^a-z0-9]+")) {
            if (w.length() > 2 && !STOPWORDS.contains(w)) words.add(w);
        }
        return words;
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
