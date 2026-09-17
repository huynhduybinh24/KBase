package com.kbase.backend.rag.embedding;

import java.util.List;

public record EmbeddingResult(List<Double> vector, String model, int dimensions) {

    public EmbeddingResult {
        vector = List.copyOf(vector);
        if (vector.size() != dimensions) {
            throw new IllegalArgumentException("Embedding dimensions do not match vector size");
        }
    }
}
