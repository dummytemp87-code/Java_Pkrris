package com.studyassistant.repository;

import com.studyassistant.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByGoalId(Long goalId);
    List<StudyPlan> findByUserIdAndDayOfWeek(Long userId, String dayOfWeek);
}
