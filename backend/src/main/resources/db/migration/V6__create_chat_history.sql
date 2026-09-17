CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    created_by UUID NOT NULL,
    title VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_chat_sessions_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sessions_created_by FOREIGN KEY (created_by)
        REFERENCES users (id)
);

CREATE INDEX idx_chat_sessions_project_user_updated
    ON chat_sessions (project_id, created_by, updated_at DESC);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    model VARCHAR(255),
    input_token_count INTEGER,
    output_token_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id)
        REFERENCES chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT ck_chat_messages_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_chat_messages_input_tokens CHECK (
        input_token_count IS NULL OR input_token_count >= 0
    ),
    CONSTRAINT ck_chat_messages_output_tokens CHECK (
        output_token_count IS NULL OR output_token_count >= 0
    )
);

CREATE INDEX idx_chat_messages_session_created
    ON chat_messages (session_id, created_at ASC);

CREATE TABLE chat_message_sources (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    chunk_id UUID,
    document_id UUID,
    document_title VARCHAR(200) NOT NULL,
    chunk_index INTEGER NOT NULL,
    similarity_score DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_chat_message_sources_message FOREIGN KEY (message_id)
        REFERENCES chat_messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_sources_chunk FOREIGN KEY (chunk_id)
        REFERENCES document_chunks (id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_message_sources_document FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE SET NULL,
    CONSTRAINT uk_chat_message_sources_message_chunk UNIQUE (message_id, chunk_id),
    CONSTRAINT ck_chat_message_sources_chunk_index CHECK (chunk_index >= 0)
);

CREATE INDEX idx_chat_message_sources_message
    ON chat_message_sources (message_id);
