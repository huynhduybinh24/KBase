package com.kbase.backend.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    boolean existsByStorageKey(String storageKey);

    List<Document> findAllByProjectIdAndStatusOrderByCreatedAtDesc(
            UUID projectId,
            DocumentStatus status
    );

    Optional<Document> findByIdAndProjectIdAndStatus(
            UUID documentId,
            UUID projectId,
            DocumentStatus status
    );
}
