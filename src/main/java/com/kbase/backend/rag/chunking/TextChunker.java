package com.kbase.backend.rag.chunking;

import java.util.List;

public interface TextChunker {
    List<TextChunk> chunk(String text);
}
