package com.studyassistant.service;

import com.studyassistant.dto.AIChatDTO;
import com.studyassistant.entity.AIChat;
import com.studyassistant.entity.Goal;
import com.studyassistant.entity.User;
import com.studyassistant.repository.AIChatRepository;
import com.studyassistant.repository.GoalRepository;
import com.studyassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AIChatService {
    @Autowired
    private AIChatRepository aiChatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    public AIChatDTO sendMessage(Long userId, AIChatDTO chatDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AIChat chat = new AIChat();
        chat.setUser(user);
        chat.setUserMessage(chatDTO.getUserMessage());
        chat.setTopic(chatDTO.getTopic());

        if (chatDTO.getGoalId() != null) {
            Goal goal = goalRepository.findById(chatDTO.getGoalId())
                    .orElseThrow(() -> new RuntimeException("Goal not found"));
            chat.setGoal(goal);
        }

        // Generate session ID if not provided
        if (chatDTO.getSessionId() == null) {
            chat.setSessionId(UUID.randomUUID().toString());
        } else {
            chat.setSessionId(chatDTO.getSessionId());
        }

        // Simulate AI response (replace with actual AI service call)
        chat.setAiResponse(generateAIResponse(chatDTO.getUserMessage(), chatDTO.getTopic()));

        AIChat savedChat = aiChatRepository.save(chat);
        return convertToDTO(savedChat);
    }

    public List<AIChatDTO> getChatHistory(Long userId, String sessionId) {
        return aiChatRepository.findByUserIdAndSessionId(userId, sessionId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AIChatDTO> getUserChats(Long userId) {
        return aiChatRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private String generateAIResponse(String userMessage, String topic) {
        // Placeholder for AI response generation
        // In production, integrate with OpenAI, Claude, or other AI service
        return "This is an AI-generated response to: " + userMessage + 
               (topic != null ? " (Topic: " + topic + ")" : "");
    }

    private AIChatDTO convertToDTO(AIChat chat) {
        return new AIChatDTO(
                chat.getId(),
                chat.getSessionId(),
                chat.getUserMessage(),
                chat.getAiResponse(),
                chat.getTopic(),
                chat.getGoal() != null ? chat.getGoal().getId() : null,
                chat.getCreatedAt()
        );
    }
}
