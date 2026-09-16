package com.kbase.backend.document.dto;

import com.kbase.backend.document.Document;
import com.kbase.backend.document.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        String originalFileName,
        String storageKey,
        String contentType,
        long fileSize,
        DocumentStatus status,
        DocumentUserResponse uploadedBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getProject().getId(),
                document.getTitle(),
                document.getDescription(),
                document.getOriginalFileName(),
                document.getStorageKey(),
                document.getContentType(),
                document.getFileSize(),
                document.getStatus(),
                DocumentUserResponse.from(document.getUploadedBy()),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
