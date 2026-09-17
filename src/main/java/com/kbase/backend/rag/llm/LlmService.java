package com.kbase.backend.rag.llm;

public interface LlmService {
    LlmResponse generate(LlmRequest request);
}
