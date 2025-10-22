package com.studyassistant.controller;

import com.studyassistant.dto.ResourceDTO;
import com.studyassistant.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/resources")
@CrossOrigin(origins = "*")
public class ResourceController {
    @Autowired
    private ResourceService resourceService;

    @PostMapping
    public ResponseEntity<ResourceDTO> createResource(@RequestBody ResourceDTO resourceDTO) {
        try {
            ResourceDTO createdResource = resourceService.createResource(resourceDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdResource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<ResourceDTO>> getResourcesByTopic(@PathVariable String topic) {
        try {
            List<ResourceDTO> resources = resourceService.getResourcesByTopic(topic);
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/type/{resourceType}")
    public ResponseEntity<List<ResourceDTO>> getResourcesByType(@PathVariable String resourceType) {
        try {
            List<ResourceDTO> resources = resourceService.getResourcesByType(resourceType);
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/difficulty/{difficultyLevel}")
    public ResponseEntity<List<ResourceDTO>> getResourcesByDifficulty(@PathVariable String difficultyLevel) {
        try {
            List<ResourceDTO> resources = resourceService.getResourcesByDifficulty(difficultyLevel);
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{resourceId}/view")
    public ResponseEntity<ResourceDTO> incrementViewCount(@PathVariable Long resourceId) {
        try {
            ResourceDTO resource = resourceService.incrementViewCount(resourceId);
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{resourceId}/rate")
    public ResponseEntity<ResourceDTO> rateResource(@PathVariable Long resourceId, @RequestParam Double rating) {
        try {
            ResourceDTO resource = resourceService.rateResource(resourceId, rating);
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
