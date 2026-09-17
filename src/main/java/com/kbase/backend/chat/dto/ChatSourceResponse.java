package com.kbase.backend.chat.dto;

import com.kbase.backend.chat.ChatMessageSource;

import java.util.UUID;

public record ChatSourceResponse(
        UUID documentId,
        String documentTitle,
        UUID chunkId,
        int chunkIndex,
        double score
) {
    public static ChatSourceResponse from(ChatMessageSource source) {
        return new ChatSourceResponse(
                source.getDocumentId(), source.getDocumentTitle(), source.getChunkId(),
                source.getChunkIndex(), source.getSimilarityScore());
    }
}
