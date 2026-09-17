package com.kbase.backend.chat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 255)
    private String model;

    @Column(name = "input_token_count")
    private Integer inputTokenCount;

    @Column(name = "output_token_count")
    private Integer outputTokenCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ChatMessageSource> sources = new ArrayList<>();

    protected ChatMessage() { }

    public ChatMessage(
            ChatSession session,
            ChatRole role,
            String content,
            String model,
            Integer inputTokenCount,
            Integer outputTokenCount
    ) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.role = role;
        this.content = content;
        this.model = model;
        this.inputTokenCount = inputTokenCount;
        this.outputTokenCount = outputTokenCount;
    }

    public void addSource(ChatMessageSource source) { sources.add(source); }
    public UUID getId() { return id; }
    public ChatSession getSession() { return session; }
    public ChatRole getRole() { return role; }
    public String getContent() { return content; }
    public String getModel() { return model; }
    public Integer getInputTokenCount() { return inputTokenCount; }
    public Integer getOutputTokenCount() { return outputTokenCount; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ChatMessageSource> getSources() { return List.copyOf(sources); }
}
