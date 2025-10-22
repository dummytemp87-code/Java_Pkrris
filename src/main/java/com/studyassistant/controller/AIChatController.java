package com.studyassistant.controller;

import com.studyassistant.dto.AIChatDTO;
import com.studyassistant.service.AIChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/ai-chat")
@CrossOrigin(origins = "*")
public class AIChatController {
    @Autowired
    private AIChatService aiChatService;

    @PostMapping("/users/{userId}")
    public ResponseEntity<AIChatDTO> sendMessage(@PathVariable Long userId, @RequestBody AIChatDTO chatDTO) {
        try {
            AIChatDTO response = aiChatService.sendMessage(userId, chatDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/users/{userId}/sessions/{sessionId}")
    public ResponseEntity<List<AIChatDTO>> getChatHistory(@PathVariable Long userId, @PathVariable String sessionId) {
        try {
            List<AIChatDTO> history = aiChatService.getChatHistory(userId, sessionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<AIChatDTO>> getUserChats(@PathVariable Long userId) {
        try {
            List<AIChatDTO> chats = aiChatService.getUserChats(userId);
            return ResponseEntity.ok(chats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
