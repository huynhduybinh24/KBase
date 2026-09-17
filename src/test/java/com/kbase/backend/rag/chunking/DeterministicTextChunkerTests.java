package com.kbase.backend.rag.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicTextChunkerTests {

    private final DeterministicTextChunker chunker =
            new DeterministicTextChunker(new RagProperties(10, 2));

    @Test
    void blankTextProducesNoChunksAndShortTextProducesOne() {
        assertTrue(chunker.chunk("  \n ").isEmpty());
        List<TextChunk> chunks = chunker.chunk("Short meaningful text.");
        assertEquals(1, chunks.size());
        assertEquals("Short meaningful text.", chunks.getFirst().content());
    }

    @Test
    void longTextIsDeterministicOrderedOverlappingAndNeverBlank() {
        String text = "First paragraph explains authentication clearly. "
                + "Second sentence contains authorization details. "
                + "Third sentence describes secure tokens and validation.";

        List<TextChunk> first = chunker.chunk(text);
        List<TextChunk> second = chunker.chunk(text);

        assertEquals(first, second);
        assertTrue(first.size() > 1);
        for (int index = 0; index < first.size(); index++) {
            assertEquals(index, first.get(index).index());
            assertFalse(first.get(index).content().isBlank());
            assertTrue(first.get(index).approximateTokenCount() > 0);
        }
        String previous = first.getFirst().content();
        String next = first.get(1).content();
        assertTrue(previous.substring(Math.max(0, previous.length() - 4)).chars()
                .anyMatch(character -> next.indexOf(character) >= 0));
    }
}
