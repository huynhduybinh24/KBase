package com.kbase.backend.document.chunk;

import com.kbase.backend.document.Document;
import com.kbase.backend.document.extraction.DocumentContent;
import com.kbase.backend.document.extraction.DocumentContentRepository;
import com.kbase.backend.document.extraction.ExtractionStatus;
import com.kbase.backend.document.extraction.IndexingStatus;
import com.kbase.backend.document.extraction.dto.DocumentContentResponse;
import com.kbase.backend.exception.ConflictException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.rag.chunking.TextChunk;
import com.kbase.backend.rag.chunking.TextChunker;
import com.kbase.backend.rag.embedding.EmbeddingResult;
import com.kbase.backend.rag.embedding.EmbeddingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DefaultDocumentChunkService implements DocumentChunkService {
    private static final Logger log = LoggerFactory.getLogger(DefaultDocumentChunkService.class);

    private final DocumentContentRepository contentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final TextChunker chunker;
    private final EmbeddingService embeddingService;

    public DefaultDocumentChunkService(
            DocumentContentRepository contentRepository,
            DocumentChunkRepository chunkRepository,
            TextChunker chunker,
            EmbeddingService embeddingService
    ) {
        this.contentRepository = contentRepository;
        this.chunkRepository = chunkRepository;
        this.chunker = chunker;
        this.embeddingService = embeddingService;
    }

    @Override
    @Transactional
    public void indexAfterExtraction(Document document, DocumentContent content) {
        if (content.getExtractionStatus() == ExtractionStatus.COMPLETED) {
            index(document, content);
        }
    }

    @Override
    @Transactional
    public DocumentContentResponse reindex(Document document) {
        DocumentContent content = contentRepository.findByDocumentIdForUpdate(document.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document content not found"));
        if (content.getExtractionStatus() != ExtractionStatus.COMPLETED) {
            throw new ConflictException("Text extraction must be completed before indexing");
        }
        if (content.getIndexingStatus() == IndexingStatus.PROCESSING) {
            throw new ConflictException("Document indexing is already processing");
        }
        index(document, content);
        return DocumentContentResponse.from(content);
    }

    private void index(Document document, DocumentContent content) {
        content.markIndexingProcessing();
        contentRepository.saveAndFlush(content);
        try {
            List<TextChunk> textChunks = chunker.chunk(content.getExtractedText());
            List<EmbeddingResult> embeddings = embeddingService.embedAll(
                    textChunks.stream().map(TextChunk::content).toList());
            if (embeddings.size() != textChunks.size()) {
                throw new IllegalStateException("Embedding count does not match chunk count");
            }
            List<ChunkEmbedding> chunks = new ArrayList<>(textChunks.size());
            for (int index = 0; index < textChunks.size(); index++) {
                TextChunk chunk = textChunks.get(index);
                chunks.add(new ChunkEmbedding(UUID.randomUUID(), chunk.index(), chunk.content(),
                        chunk.approximateTokenCount(), embeddings.get(index)));
            }
            chunkRepository.replace(document.getId(), chunks);
            content.markIndexingCompleted();
        } catch (Exception exception) {
            log.warn("Document indexing failed document={} reason={}", document.getId(),
                    exception.getClass().getSimpleName());
            chunkRepository.deleteByDocumentId(document.getId());
            content.markIndexingFailed();
        }
        contentRepository.saveAndFlush(content);
    }
}
