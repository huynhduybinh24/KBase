package com.kbase.backend.rag.embedding;

import java.util.List;

public interface EmbeddingService {

    EmbeddingResult embed(String text);

    default List<EmbeddingResult> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }
}
