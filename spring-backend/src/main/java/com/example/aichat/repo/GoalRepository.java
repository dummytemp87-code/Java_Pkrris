package com.example.aichat.repo;

import com.example.aichat.model.Goal;
import com.example.aichat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserOrderByCreatedAtDesc(User user);
    Optional<Goal> findByUserAndTitle(User user, String title);
}
