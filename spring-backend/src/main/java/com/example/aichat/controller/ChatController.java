package com.example.aichat.controller;

import com.example.aichat.dto.ChatReplyDto;
import com.example.aichat.dto.ChatRequestDto;
import com.example.aichat.dto.MessageDto;
import com.example.aichat.service.OpenAIService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai-chat")
public class ChatController {

    private final OpenAIService openAIService;

    public ChatController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = {"http://localhost:3000"}, allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<?> chat(@RequestBody ChatRequestDto req) {
        try {
            List<Map<String, String>> msgs = new ArrayList<>();
            String system = (req.getSystemPrompt() == null || req.getSystemPrompt().isBlank())
                    ? "You are a friendly, student-focused AI tutor. Write answers in clean Markdown (headings, lists, bold) with step-by-step clarity. Do NOT use LaTeX or $...$ or \\(...\\) or \\[...\\]. Use plain-text math: exponents with ^ (x^2), fractions as a/b, inline code for short expressions (like `x^2 + 1`). Keep responses concise unless asked for more detail and include small, relevant examples."
                    : req.getSystemPrompt();
            msgs.add(Map.of("role", "system", "content", system));

            if (req.getMessages() != null) {
                for (MessageDto m : req.getMessages()) {
                    if (m.getText() == null || m.getText().isBlank()) continue;
                    String role = "user";
                    if ("tutor".equalsIgnoreCase(m.getRole())) role = "assistant";
                    else if ("user".equalsIgnoreCase(m.getRole())) role = "user";
                    msgs.add(Map.of("role", role, "content", m.getText()));
                }
            }

            String reply = openAIService.getChatCompletion(msgs);
            return ResponseEntity.ok(new ChatReplyDto(reply));
        } catch (Exception ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Failed to get AI response");
            err.put("details", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }
}
