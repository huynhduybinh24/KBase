package com.kbase.backend.rag.retrieval;

import java.util.UUID;

public record SemanticSearchResult(
        UUID chunkId,
        UUID documentId,
        String documentTitle,
        int chunkIndex,
        String content,
        double score
) {
}
