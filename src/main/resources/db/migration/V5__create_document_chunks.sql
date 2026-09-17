CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE document_contents
    ADD COLUMN indexing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN indexing_error TEXT,
    ADD COLUMN indexed_at TIMESTAMP WITH TIME ZONE,
    ADD CONSTRAINT ck_document_contents_indexing_status CHECK (
        indexing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    );

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    embedding VECTOR NOT NULL,
    embedding_model VARCHAR(255) NOT NULL,
    embedding_dimensions INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_document_chunks_document FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT uk_document_chunks_document_index UNIQUE (document_id, chunk_index),
    CONSTRAINT ck_document_chunks_chunk_index CHECK (chunk_index >= 0),
    CONSTRAINT ck_document_chunks_token_count CHECK (token_count IS NULL OR token_count >= 0),
    CONSTRAINT ck_document_chunks_embedding_dimensions CHECK (embedding_dimensions > 0)
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks (document_id);
