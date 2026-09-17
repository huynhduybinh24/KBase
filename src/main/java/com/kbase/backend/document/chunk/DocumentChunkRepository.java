package com.kbase.backend.document.chunk;

import com.kbase.backend.rag.embedding.EmbeddingResult;
import com.kbase.backend.rag.retrieval.SemanticSearchResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class DocumentChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void replace(UUID documentId, List<ChunkEmbedding> chunks) {
        deleteByDocumentId(documentId);
        if (chunks.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        jdbcTemplate.batchUpdate("""
                        INSERT INTO document_chunks (
                            id, document_id, chunk_index, content, token_count, embedding,
                            embedding_model, embedding_dimensions, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?::vector, ?, ?, ?, ?)
                        """,
                chunks,
                chunks.size(),
                (PreparedStatement statement, ChunkEmbedding chunk) -> {
                    statement.setObject(1, chunk.id());
                    statement.setObject(2, documentId);
                    statement.setInt(3, chunk.chunkIndex());
                    statement.setString(4, chunk.content());
                    statement.setInt(5, chunk.approximateTokenCount());
                    statement.setString(6, vectorLiteral(chunk.embedding()));
                    statement.setString(7, chunk.embedding().model());
                    statement.setInt(8, chunk.embedding().dimensions());
                    statement.setTimestamp(9, Timestamp.from(now));
                    statement.setTimestamp(10, Timestamp.from(now));
                });
    }

    public void deleteByDocumentId(UUID documentId) {
        jdbcTemplate.update("DELETE FROM document_chunks WHERE document_id = ?", documentId);
    }

    public List<SemanticSearchResult> search(
            UUID projectId,
            UUID documentId,
            EmbeddingResult queryEmbedding,
            int topK
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.document_id, d.title, c.chunk_index, c.content,
                       1 - (c.embedding <=> ?::vector) AS score
                FROM document_chunks c
                JOIN documents d ON d.id = c.document_id
                JOIN document_contents dc ON dc.document_id = d.id
                WHERE d.project_id = ?
                  AND d.status = 'ACTIVE'
                  AND dc.indexing_status = 'COMPLETED'
                  AND c.embedding_dimensions = ?
                """);
        if (documentId != null) {
            sql.append(" AND d.id = ?");
        }
        sql.append(" ORDER BY c.embedding <=> ?::vector ASC, c.chunk_index ASC LIMIT ?");

        List<Object> parameters = new java.util.ArrayList<>();
        String vector = vectorLiteral(queryEmbedding);
        parameters.add(vector);
        parameters.add(projectId);
        parameters.add(queryEmbedding.dimensions());
        if (documentId != null) {
            parameters.add(documentId);
        }
        parameters.add(vector);
        parameters.add(topK);
        return jdbcTemplate.query(sql.toString(), (result, row) -> new SemanticSearchResult(
                result.getObject("id", UUID.class),
                result.getObject("document_id", UUID.class),
                result.getString("title"),
                result.getInt("chunk_index"),
                result.getString("content"),
                result.getDouble("score")
        ), parameters.toArray());
    }

    public int countByDocumentId(UUID documentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM document_chunks WHERE document_id = ?",
                Integer.class, documentId);
        return count == null ? 0 : count;
    }

    private String vectorLiteral(EmbeddingResult embedding) {
        return embedding.vector().stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
