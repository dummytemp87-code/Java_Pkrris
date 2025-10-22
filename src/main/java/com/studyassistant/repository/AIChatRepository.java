package com.studyassistant.repository;

import com.studyassistant.entity.AIChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AIChatRepository extends JpaRepository<AIChat, Long> {
    List<AIChat> findByUserIdAndSessionId(Long userId, String sessionId);
    List<AIChat> findByUserId(Long userId);
}
