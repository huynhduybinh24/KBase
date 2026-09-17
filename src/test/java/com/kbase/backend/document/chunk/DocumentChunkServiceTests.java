package com.kbase.backend.document.chunk;

import com.kbase.backend.document.Document;
import com.kbase.backend.document.extraction.DocumentContent;
import com.kbase.backend.document.extraction.DocumentContentRepository;
import com.kbase.backend.document.extraction.ExtractionStatus;
import com.kbase.backend.document.extraction.IndexingStatus;
import com.kbase.backend.exception.ConflictException;
import com.kbase.backend.rag.chunking.TextChunk;
import com.kbase.backend.rag.chunking.TextChunker;
import com.kbase.backend.rag.embedding.EmbeddingResult;
import com.kbase.backend.rag.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentChunkServiceTests {

    private DocumentContentRepository contentRepository;
    private DocumentChunkRepository chunkRepository;
    private TextChunker chunker;
    private EmbeddingService embeddings;
    private Document document;
    private DocumentContent content;
    private DefaultDocumentChunkService service;

    @BeforeEach
    void setUp() {
        contentRepository = mock(DocumentContentRepository.class);
        chunkRepository = mock(DocumentChunkRepository.class);
        chunker = mock(TextChunker.class);
        embeddings = mock(EmbeddingService.class);
        document = mock(Document.class);
        when(document.getId()).thenReturn(UUID.randomUUID());
        content = new DocumentContent(document);
        content.markCompleted("one two three four");
        when(contentRepository.saveAndFlush(any(DocumentContent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultDocumentChunkService(
                contentRepository, chunkRepository, chunker, embeddings);
    }

    @Test
    void completedExtractionCreatesChunksAndCompletesIndexing() {
        when(chunker.chunk(content.getExtractedText())).thenReturn(List.of(
                new TextChunk(0, "one two", 2), new TextChunk(1, "three four", 2)));
        when(embeddings.embedAll(List.of("one two", "three four"))).thenReturn(List.of(
                embedding(1), embedding(2)));

        service.indexAfterExtraction(document, content);

        assertEquals(IndexingStatus.COMPLETED, content.getIndexingStatus());
        verify(chunkRepository).replace(any(), org.mockito.ArgumentMatchers.argThat(
                chunks -> chunks.size() == 2
                        && chunks.get(0).chunkIndex() == 0
                        && chunks.get(1).chunkIndex() == 1));
    }

    @Test
    void incompleteExtractionIsNotIndexed() {
        DocumentContent pending = new DocumentContent(document);
        service.indexAfterExtraction(document, pending);
        verify(chunker, never()).chunk(any());
        verify(chunkRepository, never()).replace(any(), any());
    }

    @Test
    void embeddingFailureMarksFailedAndRemovesPartialChunks() {
        when(chunker.chunk(any())).thenReturn(List.of(new TextChunk(0, "content", 2)));
        when(embeddings.embedAll(any())).thenThrow(new IllegalStateException("provider secret"));

        service.indexAfterExtraction(document, content);

        assertEquals(IndexingStatus.FAILED, content.getIndexingStatus());
        assertEquals("Document indexing failed", content.getIndexingError());
        verify(chunkRepository).deleteByDocumentId(document.getId());
        verify(chunkRepository, never()).replace(any(), any());
    }

    @Test
    void reindexReplacesExistingChunks() {
        when(contentRepository.findByDocumentIdForUpdate(document.getId()))
                .thenReturn(Optional.of(content));
        when(chunker.chunk(any())).thenReturn(List.of(new TextChunk(0, "new content", 3)));
        when(embeddings.embedAll(any())).thenReturn(List.of(embedding(3)));

        service.reindex(document);

        verify(chunkRepository).replace(any(), org.mockito.ArgumentMatchers.argThat(
                chunks -> chunks.size() == 1 && chunks.getFirst().content().equals("new content")));
        assertEquals(IndexingStatus.COMPLETED, content.getIndexingStatus());
    }

    @Test
    void extractionMustBeCompletedBeforeManualIndexing() {
        DocumentContent pending = new DocumentContent(document);
        when(contentRepository.findByDocumentIdForUpdate(document.getId()))
                .thenReturn(Optional.of(pending));
        assertEquals(ExtractionStatus.PENDING, pending.getExtractionStatus());
        assertThrows(ConflictException.class, () -> service.reindex(document));
    }

    private EmbeddingResult embedding(double value) {
        return new EmbeddingResult(List.of(value, 0.0), "test", 2);
    }
}
