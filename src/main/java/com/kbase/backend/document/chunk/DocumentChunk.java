package com.kbase.backend.document.chunk;

import java.time.Instant;
import java.util.UUID;

public record DocumentChunk(
        UUID id,
        UUID documentId,
        int chunkIndex,
        String content,
        Integer tokenCount,
        String embeddingModel,
        int embeddingDimensions,
        Instant createdAt,
        Instant updatedAt
) {
}
