package com.studyassistant.service;

import com.studyassistant.dto.ReminderDTO;
import com.studyassistant.entity.Reminder;
import com.studyassistant.entity.User;
import com.studyassistant.repository.ReminderRepository;
import com.studyassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReminderService {
    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private UserRepository userRepository;

    public ReminderDTO createReminder(Long userId, ReminderDTO reminderDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setTitle(reminderDTO.getTitle());
        reminder.setDescription(reminderDTO.getDescription());
        reminder.setReminderTime(reminderDTO.getReminderTime());
        reminder.setReminderType(reminderDTO.getReminderType());
        reminder.setIsSent(false);

        Reminder savedReminder = reminderRepository.save(reminder);
        return convertToDTO(savedReminder);
    }

    public ReminderDTO updateReminder(Long reminderId, ReminderDTO reminderDTO) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));

        reminder.setTitle(reminderDTO.getTitle());
        reminder.setDescription(reminderDTO.getDescription());
        reminder.setReminderTime(reminderDTO.getReminderTime());
        reminder.setReminderType(reminderDTO.getReminderType());

        Reminder updatedReminder = reminderRepository.save(reminder);
        return convertToDTO(updatedReminder);
    }

    public List<ReminderDTO> getUserReminders(Long userId) {
        return reminderRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ReminderDTO> getPendingReminders(Long userId) {
        return reminderRepository.findByUserIdAndIsSent(userId, false).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ReminderDTO markReminderSent(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));

        reminder.setIsSent(true);
        Reminder updatedReminder = reminderRepository.save(reminder);
        return convertToDTO(updatedReminder);
    }

    public void deleteReminder(Long reminderId) {
        reminderRepository.deleteById(reminderId);
    }

    private ReminderDTO convertToDTO(Reminder reminder) {
        return new ReminderDTO(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getReminderTime(),
                reminder.getIsSent(),
                reminder.getReminderType(),
                reminder.getCreatedAt()
        );
    }
}
