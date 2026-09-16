package com.kbase.backend.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 500) String originalFileName,
        @NotBlank @Size(max = 1024) String storageKey,
        @NotBlank @Size(max = 255) String contentType,
        @Positive long fileSize
) {
}
