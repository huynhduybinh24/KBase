package com.kbase.backend.chat.dto;

import com.kbase.backend.chat.ChatSession;
import org.springframework.data.domain.Page;

import java.util.List;

public record ChatSessionPageResponse(
        List<ChatSessionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ChatSessionPageResponse from(Page<ChatSession> page) {
        return new ChatSessionPageResponse(
                page.getContent().stream().map(ChatSessionResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
