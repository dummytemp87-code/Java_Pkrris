package com.studyassistant.controller;

import com.studyassistant.dto.NoteDTO;
import com.studyassistant.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/notes")
@CrossOrigin(origins = "*")
public class NoteController {
    @Autowired
    private NoteService noteService;

    @PostMapping("/users/{userId}")
    public ResponseEntity<NoteDTO> createNote(@PathVariable Long userId, @RequestBody NoteDTO noteDTO) {
        try {
            NoteDTO createdNote = noteService.createNote(userId, noteDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdNote);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<NoteDTO> updateNote(@PathVariable Long noteId, @RequestBody NoteDTO noteDTO) {
        try {
            NoteDTO updatedNote = noteService.updateNote(noteId, noteDTO);
            return ResponseEntity.ok(updatedNote);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<NoteDTO>> getUserNotes(@PathVariable Long userId) {
        try {
            List<NoteDTO> notes = noteService.getUserNotes(userId);
            return ResponseEntity.ok(notes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}/goals/{goalId}")
    public ResponseEntity<List<NoteDTO>> getGoalNotes(@PathVariable Long userId, @PathVariable Long goalId) {
        try {
            List<NoteDTO> notes = noteService.getGoalNotes(userId, goalId);
            return ResponseEntity.ok(notes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{userId}/topic/{topic}")
    public ResponseEntity<List<NoteDTO>> getTopicNotes(@PathVariable Long userId, @PathVariable String topic) {
        try {
            List<NoteDTO> notes = noteService.getTopicNotes(userId, topic);
            return ResponseEntity.ok(notes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId) {
        try {
            noteService.deleteNote(noteId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
