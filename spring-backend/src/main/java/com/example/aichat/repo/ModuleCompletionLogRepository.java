package com.example.aichat.repo;

import com.example.aichat.model.ModuleCompletionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ModuleCompletionLogRepository extends JpaRepository<ModuleCompletionLog, Long> {
    @Query("select coalesce(sum(l.minutes),0) from ModuleCompletionLog l where l.user.id = :userId and l.completedDate = :date")
    Integer sumMinutesByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("select count(l.id) from ModuleCompletionLog l where l.user.id = :userId and l.completedDate = :date")
    Integer countByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    List<ModuleCompletionLog> findByUserIdAndCompletedDateBetween(Long userId, LocalDate start, LocalDate end);

    Optional<ModuleCompletionLog> findByUserIdAndGoalTitleAndModuleTitleAndCompletedDate(Long userId, String goalTitle, String moduleTitle, LocalDate completedDate);

    @Modifying
    @Query("delete from ModuleCompletionLog l where l.user.id = :userId and l.goalTitle = :goalTitle")
    void deleteByUserIdAndGoalTitle(@Param("userId") Long userId, @Param("goalTitle") String goalTitle);
}
