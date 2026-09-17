package com.kbase.backend.rag.llm;

public class LlmUnavailableException extends RuntimeException {
    public LlmUnavailableException(Throwable cause) {
        super("Language model provider is unavailable", cause);
    }
}
