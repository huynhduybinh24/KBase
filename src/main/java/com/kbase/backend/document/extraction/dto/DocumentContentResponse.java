package com.kbase.backend.document.extraction.dto;

import com.kbase.backend.document.extraction.DocumentContent;
import com.kbase.backend.document.extraction.ExtractionStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentContentResponse(
        UUID documentId,
        ExtractionStatus status,
        String text,
        String error,
        Instant extractedAt
) {
    public static DocumentContentResponse from(DocumentContent content) {
        return new DocumentContentResponse(
                content.getDocument().getId(), content.getExtractionStatus(),
                content.getExtractedText(), content.getExtractionError(), content.getExtractedAt());
    }
}
