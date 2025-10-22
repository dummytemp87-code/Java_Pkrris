package com.studyassistant.repository;

import com.studyassistant.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByUserId(Long userId);
    List<Note> findByUserIdAndGoalId(Long userId, Long goalId);
    List<Note> findByUserIdAndTopic(Long userId, String topic);
}
