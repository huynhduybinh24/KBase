package com.kbase.backend.document.chunk;

import com.kbase.backend.rag.embedding.EmbeddingResult;

import java.util.UUID;

public record ChunkEmbedding(
        UUID id,
        int chunkIndex,
        String content,
        int approximateTokenCount,
        EmbeddingResult embedding
) {
}
