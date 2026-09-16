CREATE TABLE documents (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    uploaded_by UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000),
    original_file_name VARCHAR(500) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_documents_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id),
    CONSTRAINT uk_documents_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_documents_file_size CHECK (file_size > 0),
    CONSTRAINT ck_documents_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

CREATE INDEX idx_documents_project_status ON documents (project_id, status);
CREATE INDEX idx_documents_uploaded_by ON documents (uploaded_by);
