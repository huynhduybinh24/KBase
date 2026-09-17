package com.kbase.backend.rag.chunking;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@EnableConfigurationProperties(RagProperties.class)
public class DeterministicTextChunker implements TextChunker {

    private static final int APPROXIMATE_CHARACTERS_PER_TOKEN = 4;
    private final int maxCharacters;
    private final int overlapCharacters;

    public DeterministicTextChunker(RagProperties properties) {
        if (properties.chunkSize() <= 0 || properties.chunkOverlap() < 0
                || properties.chunkOverlap() >= properties.chunkSize()) {
            throw new IllegalArgumentException("Chunk size and overlap configuration is invalid");
        }
        maxCharacters = properties.chunkSize() * APPROXIMATE_CHARACTERS_PER_TOKEN;
        overlapCharacters = properties.chunkOverlap() * APPROXIMATE_CHARACTERS_PER_TOKEN;
    }

    @Override
    public List<TextChunk> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replace("\r\n", "\n").trim();
        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int hardEnd = Math.min(normalized.length(), start + maxCharacters);
            int end = hardEnd == normalized.length()
                    ? hardEnd
                    : preferredBoundary(normalized, start, hardEnd);
            String content = normalized.substring(start, end).trim();
            if (!content.isBlank()) {
                chunks.add(new TextChunk(chunks.size(), content,
                        Math.max(1, (content.length() + 3) / 4)));
            }
            if (end == normalized.length()) {
                break;
            }
            int next = Math.max(start + 1, end - overlapCharacters);
            while (next < end && Character.isWhitespace(normalized.charAt(next))) {
                next++;
            }
            start = next;
        }
        return List.copyOf(chunks);
    }

    private int preferredBoundary(String text, int start, int hardEnd) {
        int minimum = start + maxCharacters / 2;
        int paragraph = text.lastIndexOf("\n\n", hardEnd);
        if (paragraph >= minimum) {
            return paragraph + 2;
        }
        int sentence = text.lastIndexOf(". ", hardEnd);
        if (sentence >= minimum) {
            return sentence + 1;
        }
        for (int index = hardEnd; index >= minimum; index--) {
            if (Character.isWhitespace(text.charAt(index - 1))) {
                return index;
            }
        }
        return hardEnd;
    }
}
