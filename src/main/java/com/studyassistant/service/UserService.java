package com.studyassistant.service;

import com.studyassistant.dto.UserDTO;
import com.studyassistant.dto.UserSettingsDTO;
import com.studyassistant.entity.User;
import com.studyassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public UserDTO createUser(UserDTO userDTO) {
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword()); // In production, hash the password
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setThemePreference(userDTO.getThemePreference() != null ? userDTO.getThemePreference() : "light");
        user.setNotificationEnabled(userDTO.getNotificationEnabled() != null ? userDTO.getNotificationEnabled() : true);
        user.setDailyGoalMinutes(userDTO.getDailyGoalMinutes() != null ? userDTO.getDailyGoalMinutes() : 60);

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    public UserDTO updateUserProfile(Long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public UserSettingsDTO updateUserSettings(Long userId, UserSettingsDTO settingsDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setThemePreference(settingsDTO.getThemePreference());
        user.setNotificationEnabled(settingsDTO.getNotificationEnabled());
        user.setDailyGoalMinutes(settingsDTO.getDailyGoalMinutes());
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return convertSettingsToDTO(updatedUser);
    }

    public UserSettingsDTO getUserSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertSettingsToDTO(user);
    }

    public UserDTO updateTheme(Long userId, String theme) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setThemePreference(theme);
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public UserDTO toggleNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setNotificationEnabled(!user.getNotificationEnabled());
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public UserDTO updateDailyGoal(Long userId, Integer minutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setDailyGoalMinutes(minutes);
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getThemePreference(),
                user.getNotificationEnabled(),
                user.getDailyGoalMinutes(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private UserSettingsDTO convertSettingsToDTO(User user) {
        return new UserSettingsDTO(
                user.getId(),
                user.getThemePreference(),
                user.getNotificationEnabled(),
                true, // emailNotifications - can be extended with separate fields
                true, // pushNotifications - can be extended with separate fields
                user.getDailyGoalMinutes(),
                "en", // language - can be extended with separate field
                false // twoFactorEnabled - can be extended with separate field
        );
    }
}
