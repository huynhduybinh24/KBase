package com.kbase.backend.chat.dto;

import com.kbase.backend.chat.ChatMessage;
import com.kbase.backend.chat.ChatRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        ChatRole role,
        String content,
        Instant createdAt,
        String model,
        Integer inputTokenCount,
        Integer outputTokenCount,
        List<ChatSourceResponse> sources
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(), message.getRole(), message.getContent(), message.getCreatedAt(),
                message.getModel(), message.getInputTokenCount(), message.getOutputTokenCount(),
                message.getSources().stream().map(ChatSourceResponse::from).toList());
    }
}
