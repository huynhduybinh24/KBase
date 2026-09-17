package com.kbase.backend.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Page<ChatSession> findAllByProjectIdAndCreatedById(
            UUID projectId, UUID createdById, Pageable pageable);

    Optional<ChatSession> findByIdAndProjectIdAndCreatedById(
            UUID sessionId, UUID projectId, UUID createdById);
}
