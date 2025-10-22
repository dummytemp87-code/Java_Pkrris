package com.studyassistant.repository;

import com.studyassistant.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByTopic(String topic);
    List<Resource> findByResourceType(String resourceType);
    List<Resource> findByDifficultyLevel(String difficultyLevel);
}
