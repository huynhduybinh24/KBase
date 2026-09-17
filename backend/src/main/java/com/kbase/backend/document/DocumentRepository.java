package com.kbase.backend.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID>,
        JpaSpecificationExecutor<Document> {

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
