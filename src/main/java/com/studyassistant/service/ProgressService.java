package com.studyassistant.service;

import com.studyassistant.dto.ProgressDTO;
import com.studyassistant.entity.Progress;
import com.studyassistant.entity.User;
import com.studyassistant.repository.ProgressRepository;
import com.studyassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProgressService {
    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private UserRepository userRepository;

    public ProgressDTO recordProgress(Long userId, ProgressDTO progressDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Progress progress = new Progress();
        progress.setUser(user);
        progress.setStudyDate(progressDTO.getStudyDate());
        progress.setStudyTimeMinutes(progressDTO.getStudyTimeMinutes());
        progress.setTasksCompleted(progressDTO.getTasksCompleted());
        progress.setOverallProgressPercentage(progressDTO.getOverallProgressPercentage());
        progress.setStreakDays(progressDTO.getStreakDays());

        Progress savedProgress = progressRepository.save(progress);
        return convertToDTO(savedProgress);
    }

    public List<ProgressDTO> getUserProgress(Long userId) {
        return progressRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ProgressDTO> getProgressRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return progressRepository.findByUserIdAndStudyDateBetween(userId, startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ProgressDTO convertToDTO(Progress progress) {
        return new ProgressDTO(
                progress.getId(),
                progress.getStudyDate(),
                progress.getStudyTimeMinutes(),
                progress.getTasksCompleted(),
                progress.getOverallProgressPercentage(),
                progress.getStreakDays()
        );
    }
}
