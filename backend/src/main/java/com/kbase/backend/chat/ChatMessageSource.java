package com.kbase.backend.chat;

import com.kbase.backend.rag.retrieval.SemanticSearchResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_message_sources")
public class ChatMessageSource {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @Column(name = "chunk_id")
    private UUID chunkId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "document_title", nullable = false, length = 200)
    private String documentTitle;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "similarity_score", nullable = false)
    private double similarityScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessageSource() { }

    public ChatMessageSource(ChatMessage message, SemanticSearchResult result) {
        this.id = UUID.randomUUID();
        this.message = message;
        this.chunkId = result.chunkId();
        this.documentId = result.documentId();
        this.documentTitle = result.documentTitle();
        this.chunkIndex = result.chunkIndex();
        this.similarityScore = result.score();
    }

    public UUID getId() { return id; }
    public UUID getChunkId() { return chunkId; }
    public UUID getDocumentId() { return documentId; }
    public String getDocumentTitle() { return documentTitle; }
    public int getChunkIndex() { return chunkIndex; }
    public double getSimilarityScore() { return similarityScore; }
    public Instant getCreatedAt() { return createdAt; }
}
