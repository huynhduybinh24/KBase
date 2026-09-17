package com.kbase.backend.document.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record DocumentPageResponse(
        List<DocumentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static DocumentPageResponse from(Page<com.kbase.backend.document.Document> page) {
        return new DocumentPageResponse(
                page.getContent().stream().map(DocumentResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
