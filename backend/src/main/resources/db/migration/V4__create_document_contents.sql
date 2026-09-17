CREATE TABLE document_contents (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    extraction_status VARCHAR(20) NOT NULL,
    extracted_text TEXT,
    extraction_error TEXT,
    extracted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_document_contents_document FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT uk_document_contents_document UNIQUE (document_id),
    CONSTRAINT ck_document_contents_status CHECK (
        extraction_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'UNSUPPORTED')
    )
);

CREATE INDEX idx_documents_project_status_created_at
    ON documents (project_id, status, created_at DESC);
