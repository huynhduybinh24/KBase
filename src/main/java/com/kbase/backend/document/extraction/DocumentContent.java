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

    public UUID getId() { return id; }
    public Document getDocument() { return document; }
    public ExtractionStatus getExtractionStatus() { return extractionStatus; }
    public String getExtractedText() { return extractedText; }
    public String getExtractionError() { return extractionError; }
    public Instant getExtractedAt() { return extractedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
