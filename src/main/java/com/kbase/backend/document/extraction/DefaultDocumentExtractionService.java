package com.kbase.backend.document.extraction;

import com.kbase.backend.document.Document;
import com.kbase.backend.document.extraction.dto.DocumentContentResponse;
import com.kbase.backend.exception.ConflictException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.storage.StorageService;
import com.kbase.backend.document.chunk.DocumentChunkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DefaultDocumentExtractionService implements DocumentExtractionService {
    private static final Logger log = LoggerFactory.getLogger(DefaultDocumentExtractionService.class);

    private final DocumentContentRepository repository;
    private final StorageService storageService;
    private final List<TextExtractor> extractors;
    private final DocumentChunkService chunkService;

    public DefaultDocumentExtractionService(
            DocumentContentRepository repository,
            StorageService storageService,
            List<TextExtractor> extractors,
            DocumentChunkService chunkService
    ) {
        this.repository = repository;
        this.storageService = storageService;
        this.extractors = extractors;
        this.chunkService = chunkService;
    }

    @Override
    @Transactional
    public void initializeAndExtract(Document document) {
        DocumentContent content = repository.saveAndFlush(new DocumentContent(document));
        extract(content, document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentContentResponse get(UUID documentId) {
        return DocumentContentResponse.from(repository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document content not found")));
    }

    @Override
    @Transactional
    public DocumentContentResponse retry(Document document) {
        DocumentContent content = repository.findByDocumentIdForUpdate(document.getId())
                .orElseGet(() -> repository.saveAndFlush(new DocumentContent(document)));
        if (content.getExtractionStatus() == ExtractionStatus.PROCESSING) {
            throw new ConflictException("Document extraction is already processing");
        }
        if (content.getExtractionStatus() == ExtractionStatus.COMPLETED) {
            throw new ConflictException("Document extraction is already complete");
        }
        extract(content, document);
        return DocumentContentResponse.from(content);
    }

    private void extract(DocumentContent content, Document document) {
        TextExtractor extractor = extractors.stream()
                .filter(candidate -> candidate.supports(document.getContentType()))
                .findFirst()
                .orElse(null);
        if (extractor == null) {
            content.markUnsupported();
            repository.saveAndFlush(content);
            return;
        }

        content.markProcessing();
        repository.saveAndFlush(content);
        try (InputStream input = storageService.download(document.getStorageKey())) {
            content.markCompleted(extractor.extract(input));
        } catch (Exception exception) {
            log.warn("Document extraction failed document={} reason={}", document.getId(),
                    exception.getClass().getSimpleName());
            content.markFailed();
        }
        repository.saveAndFlush(content);
        if (content.getExtractionStatus() == ExtractionStatus.COMPLETED) {
            chunkService.indexAfterExtraction(document, content);
        }
    }
}
