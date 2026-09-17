package com.kbase.backend.rag.chunking;

public record TextChunk(int index, String content, int approximateTokenCount) {
}
