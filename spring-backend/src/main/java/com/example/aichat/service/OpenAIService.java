package com.example.aichat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAIService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.apiKey:}")
    private String apiKeyFromProps;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${groq.apiKey:}")
    private String groqApiKeyFromProps;

    @Value("${groq.baseUrl:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    @Value("${gemini.model:gemini-3.1-flash-lite}")
    private String geminiModel;

    @Value("${gemini.apiKey:}")
    private String geminiApiKeyFromProps;

    @Value("${gemini.baseUrl:https://generativelanguage.googleapis.com/v1}")
    private String geminiBaseUrl;

    private String getApiKey() {
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isBlank()) return env;
        return apiKeyFromProps;
    }

    private String getGroqApiKey() {
        String env = System.getenv("GROQ_API_KEY");
        if (env != null && !env.isBlank()) return env;
        return groqApiKeyFromProps;
    }

    private String getGeminiApiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) return env;
        return geminiApiKeyFromProps;
    }

    public String getChatCompletion(List<Map<String, String>> messages) throws Exception {
        String key = getGeminiApiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Missing GEMINI_API_KEY. Set environment variable or property gemini.apiKey");
        }
        return callGemini(messages, key);
    }

    public String getChatCompletionWithImage(List<Map<String, String>> messages, byte[] imageBytes, String mimeType) throws Exception {
        String key = getGeminiApiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Missing GEMINI_API_KEY. Set environment variable or property gemini.apiKey");
        }
        return callGeminiWithImage(messages, imageBytes, mimeType, key);
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

        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> m : messages) {
            String role = m.getOrDefault("role", "user");
            String text = m.getOrDefault("content", "");
            if (text == null || text.isBlank()) continue;
            String geminiRole = "assistant".equalsIgnoreCase(role) ? "model" : "user";
            Map<String, Object> content = new HashMap<>();
            content.put("role", geminiRole);
            content.put("parts", List.of(Map.of("text", text)));
            contents.add(content);
        }

        Map<String, Object> payload = new HashMap<>();
        if (!contents.isEmpty()) payload.put("contents", contents);
        Map<String, Object> genCfg = new HashMap<>();
        genCfg.put("temperature", 0.7);
        genCfg.put("maxOutputTokens", 6000);
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

    private String callGeminiWithImage(List<Map<String, String>> messages, byte[] imageBytes, String mimeType, String geminiKey) throws Exception {
        String url = geminiBaseUrl + "/models/" + geminiModel + ":generateContent?key=" + geminiKey;

        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> m : messages) {
            String role = m.getOrDefault("role", "user");
            String text = m.getOrDefault("content", "");
            if (text == null || text.isBlank()) continue;
            String geminiRole = "assistant".equalsIgnoreCase(role) ? "model" : "user";
            List<Object> parts = new ArrayList<>();
            parts.add(Map.of("text", text));
            Map<String, Object> content = new HashMap<>();
            content.put("role", geminiRole);
            content.put("parts", parts);
            contents.add(content);
        }
        // Attach the image to the last (user) turn so the model reads it alongside its instructions.
        if (!contents.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Object> lastParts = (List<Object>) contents.get(contents.size() - 1).get("parts");
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", Base64.getEncoder().encodeToString(imageBytes));
            lastParts.add(Map.of("inline_data", inlineData));
        }

        Map<String, Object> payload = new HashMap<>();
        if (!contents.isEmpty()) payload.put("contents", contents);
        Map<String, Object> genCfg = new HashMap<>();
        genCfg.put("temperature", 0.7);
        genCfg.put("maxOutputTokens", 6000);
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

    private String callGroq(List<Map<String, String>> messages, String groqKey) throws Exception {
        String url = groqBaseUrl + "/chat/completions";

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", groqModel);
        payload.put("messages", messages);
        payload.put("temperature", 0.7);
        payload.put("max_tokens", 6000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Groq error: " + resp.getStatusCode());
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
}