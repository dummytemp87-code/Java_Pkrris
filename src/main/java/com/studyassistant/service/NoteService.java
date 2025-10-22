package com.studyassistant.service;

import com.studyassistant.dto.NoteDTO;
import com.studyassistant.entity.Goal;
import com.studyassistant.entity.Note;
import com.studyassistant.entity.User;
import com.studyassistant.repository.GoalRepository;
import com.studyassistant.repository.NoteRepository;
import com.studyassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {
    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    public NoteDTO createNote(Long userId, NoteDTO noteDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Note note = new Note();
        note.setUser(user);
        note.setTitle(noteDTO.getTitle());
        note.setContent(noteDTO.getContent());
        note.setTopic(noteDTO.getTopic());

        if (noteDTO.getGoalId() != null) {
            Goal goal = goalRepository.findById(noteDTO.getGoalId())
                    .orElseThrow(() -> new RuntimeException("Goal not found"));
            note.setGoal(goal);
        }

        Note savedNote = noteRepository.save(note);
        return convertToDTO(savedNote);
    }

    public NoteDTO updateNote(Long noteId, NoteDTO noteDTO) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        note.setTitle(noteDTO.getTitle());
        note.setContent(noteDTO.getContent());
        note.setTopic(noteDTO.getTopic());
        note.setUpdatedAt(LocalDateTime.now());

        Note updatedNote = noteRepository.save(note);
        return convertToDTO(updatedNote);
    }

    public List<NoteDTO> getUserNotes(Long userId) {
        return noteRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<NoteDTO> getGoalNotes(Long userId, Long goalId) {
        return noteRepository.findByUserIdAndGoalId(userId, goalId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<NoteDTO> getTopicNotes(Long userId, String topic) {
        return noteRepository.findByUserIdAndTopic(userId, topic).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void deleteNote(Long noteId) {
        noteRepository.deleteById(noteId);
    }

    private NoteDTO convertToDTO(Note note) {
        return new NoteDTO(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getTopic(),
                note.getGoal() != null ? note.getGoal().getId() : null,
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
