package com.studyassistant.service;

import com.studyassistant.dto.ResourceDTO;
import com.studyassistant.entity.Resource;
import com.studyassistant.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {
    @Autowired
    private ResourceRepository resourceRepository;

    public ResourceDTO createResource(ResourceDTO resourceDTO) {
        Resource resource = new Resource();
        resource.setTitle(resourceDTO.getTitle());
        resource.setDescription(resourceDTO.getDescription());
        resource.setResourceType(resourceDTO.getResourceType());
        resource.setUrl(resourceDTO.getUrl());
        resource.setTopic(resourceDTO.getTopic());
        resource.setDifficultyLevel(resourceDTO.getDifficultyLevel());
        resource.setRating(0.0);
        resource.setViewCount(0);

        Resource savedResource = resourceRepository.save(resource);
        return convertToDTO(savedResource);
    }

    public List<ResourceDTO> getResourcesByTopic(String topic) {
        return resourceRepository.findByTopic(topic).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ResourceDTO> getResourcesByType(String resourceType) {
        return resourceRepository.findByResourceType(resourceType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ResourceDTO> getResourcesByDifficulty(String difficultyLevel) {
        return resourceRepository.findByDifficultyLevel(difficultyLevel).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ResourceDTO incrementViewCount(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        resource.setViewCount(resource.getViewCount() + 1);
        Resource updatedResource = resourceRepository.save(resource);
        return convertToDTO(updatedResource);
    }

    public ResourceDTO rateResource(Long resourceId, Double rating) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        resource.setRating(rating);
        Resource updatedResource = resourceRepository.save(resource);
        return convertToDTO(updatedResource);
    }

    private ResourceDTO convertToDTO(Resource resource) {
        return new ResourceDTO(
                resource.getId(),
                resource.getTitle(),
                resource.getDescription(),
                resource.getResourceType(),
                resource.getUrl(),
                resource.getTopic(),
                resource.getDifficultyLevel(),
                resource.getRating(),
                resource.getViewCount()
        );
    }
}
