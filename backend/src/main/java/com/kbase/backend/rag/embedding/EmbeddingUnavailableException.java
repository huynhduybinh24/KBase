package com.kbase.backend.rag.embedding;

public class EmbeddingUnavailableException extends RuntimeException {

    public EmbeddingUnavailableException(Throwable cause) {
        super("Embedding provider is unavailable", cause);
    }
}
