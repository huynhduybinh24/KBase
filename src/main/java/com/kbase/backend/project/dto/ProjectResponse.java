package com.kbase.backend.project.dto;

import com.kbase.backend.project.Project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        ProjectUserResponse owner,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                ProjectUserResponse.from(project.getOwner()),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
