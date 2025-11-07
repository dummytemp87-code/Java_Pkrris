package com.example.aichat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.*;

@Service
public class OpenAIService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.apiKey:}")
    private String apiKeyFromProps;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${gemini.apiKey:}")
    private String geminiApiKeyFromProps;
    
    @Value("${gemini.apiKeys:}")
    private String geminiApiKeysFromProps;
    
    @Value("${gemini.baseUrl:https://generativelanguage.googleapis.com/v1}")
    private String geminiBaseUrl;

    private String getApiKey() {
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isBlank()) return env;
        return apiKeyFromProps;
    }

    private String getGeminiApiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) return env;
        return geminiApiKeyFromProps;
    }

    private List<String> getGeminiApiKeys() {
        String raw = System.getenv("GEMINI_API_KEYS");
        if (raw == null || raw.isBlank()) raw = geminiApiKeysFromProps;
        List<String> keys = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String s : raw.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) keys.add(t);
            }
        }
        String single = getGeminiApiKey();
        if (single != null && !single.isBlank()) {
            if (keys.isEmpty() || !keys.contains(single)) keys.add(single);
        }
        return keys;
    }

    public String getChatCompletion(List<Map<String, String>> messages) throws Exception {
        List<String> keys = getGeminiApiKeys();
        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException("Missing GEMINI_API_KEY. Set environment variable or property gemini.apiKey");
        }
        Exception last = null;
        for (String k : keys) {
            try {
                return callGemini(messages, k);
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    last = e;
                    continue;
                }
                String body = e.getResponseBodyAsString();
                if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    if (body != null) {
                        String lower = body.toLowerCase();
                        if (lower.contains("quota") || lower.contains("exceed") || lower.contains("exhausted") || lower.contains("rate")) {
                            last = e;
                            continue;
                        }
                    }
                }
                throw e;
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) throw last;
        return "I'm sorry, I couldn't generate a response just now.";
    }

    private String callOpenAI(List<Map<String, String>> messages, String apiKey) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("OpenAI error: " + resp.getStatusCode());
        }
        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "I'm sorry, I couldn't generate a response just now.";
        }
        String content = choices.get(0).path("message").path("content").asText("");
        if (content == null || content.isBlank()) {
            return "I'm sorry, I couldn't generate a response just now.";
        }
        return content.trim();
    }

    private String callGemini(List<Map<String, String>> messages, String geminiKey) throws Exception {
        String url = geminiBaseUrl + "/models/" + geminiModel + ":generateContent?key=" + geminiKey;

        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();

        for (Map<String, String> m : messages) {
            String role = m.getOrDefault("role", "user");
            String text = m.getOrDefault("content", "");
            if (text == null || text.isBlank()) continue;
            if ("system".equalsIgnoreCase(role)) {
                Map<String, Object> parts = Map.of("text", text);
                Map<String, Object> content = new HashMap<>();
                content.put("role", "user");
                content.put("parts", List.of(parts));
                contents.add(content);
            } else {
                String gemRole = "user";
                if ("assistant".equalsIgnoreCase(role)) gemRole = "model";
                Map<String, Object> parts = Map.of("text", text);
                Map<String, Object> content = new HashMap<>();
                content.put("role", gemRole);
                content.put("parts", List.of(parts));
                contents.add(content);
            }
        }

        if (!contents.isEmpty()) {
            payload.put("contents", contents);
        }
        Map<String, Object> genCfg = new HashMap<>();
        genCfg.put("temperature", 0.7);
        payload.put("generationConfig", genCfg);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Gemini error: " + resp.getStatusCode());
        }
        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return "I'm sorry, I couldn't generate a response just now.";
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        String contentText = "";
        if (parts.isArray() && parts.size() > 0) {
            contentText = parts.get(0).path("text").asText("");
        }
        if (contentText == null || contentText.isBlank()) {
            return "I'm sorry, I couldn't generate a response just now.";
        }
        return contentText.trim();
    }
}