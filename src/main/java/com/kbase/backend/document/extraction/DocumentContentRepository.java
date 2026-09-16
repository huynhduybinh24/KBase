package com.kbase.backend.document.extraction;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentContentRepository extends JpaRepository<DocumentContent, UUID> {

    Optional<DocumentContent> findByDocumentId(UUID documentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select content from DocumentContent content where content.document.id = :documentId")
    Optional<DocumentContent> findByDocumentIdForUpdate(@Param("documentId") UUID documentId);
}
