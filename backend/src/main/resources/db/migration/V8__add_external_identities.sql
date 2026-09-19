ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

CREATE TABLE user_external_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(320),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_external_identity_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uq_external_identity_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_external_identity_user ON user_external_identities(user_id);
