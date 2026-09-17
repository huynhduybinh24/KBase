package com.kbase.backend.document.extraction;

import com.kbase.backend.document.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_contents")
public class DocumentContent {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 20)
    private ExtractionStatus extractionStatus;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "extraction_error", columnDefinition = "TEXT")
    private String extractionError;

    @Column(name = "extracted_at")
    private Instant extractedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "indexing_status", nullable = false, length = 20)
    private IndexingStatus indexingStatus = IndexingStatus.PENDING;

    @Column(name = "indexing_error", columnDefinition = "TEXT")
    private String indexingError;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentContent() {
    }

    public DocumentContent(Document document) {
        this.id = UUID.randomUUID();
        this.document = document;
        this.extractionStatus = ExtractionStatus.PENDING;
        this.indexingStatus = IndexingStatus.PENDING;
    }

    public void markProcessing() {
        extractionStatus = ExtractionStatus.PROCESSING;
        extractedText = null;
        extractionError = null;
        extractedAt = null;
    }

    public void markCompleted(String text) {
        extractionStatus = ExtractionStatus.COMPLETED;
        extractedText = text;
        extractionError = null;
        extractedAt = Instant.now();
        resetIndexing();
    }

    public void markFailed() {
        extractionStatus = ExtractionStatus.FAILED;
        extractedText = null;
        extractionError = "Text extraction failed";
        extractedAt = null;
    }

    public void markUnsupported() {
        extractionStatus = ExtractionStatus.UNSUPPORTED;
        extractedText = null;
        extractionError = null;
        extractedAt = null;
    }

    public void markIndexingProcessing() {
        indexingStatus = IndexingStatus.PROCESSING;
        indexingError = null;
        indexedAt = null;
    }

    public void markIndexingCompleted() {
        indexingStatus = IndexingStatus.COMPLETED;
        indexingError = null;
        indexedAt = Instant.now();
    }

    public void markIndexingFailed() {
        indexingStatus = IndexingStatus.FAILED;
        indexingError = "Document indexing failed";
        indexedAt = null;
    }

    public void resetIndexing() {
        indexingStatus = IndexingStatus.PENDING;
        indexingError = null;
        indexedAt = null;
    }

    public UUID getId() { return id; }
    public Document getDocument() { return document; }
    public ExtractionStatus getExtractionStatus() { return extractionStatus; }
    public String getExtractedText() { return extractedText; }
    public String getExtractionError() { return extractionError; }
    public Instant getExtractedAt() { return extractedAt; }
    public IndexingStatus getIndexingStatus() { return indexingStatus; }
    public String getIndexingError() { return indexingError; }
    public Instant getIndexedAt() { return indexedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
