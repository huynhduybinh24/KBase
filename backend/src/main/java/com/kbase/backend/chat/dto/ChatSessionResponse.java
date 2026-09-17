package com.kbase.backend.chat.dto;

import com.kbase.backend.chat.ChatSession;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionResponse(
        UUID id,
        UUID projectId,
        UUID createdBy,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(), session.getProject().getId(), session.getCreatedBy().getId(),
                session.getTitle(), session.getCreatedAt(), session.getUpdatedAt());
    }
}
