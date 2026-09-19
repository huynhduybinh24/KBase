CREATE TABLE user_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    full_name VARCHAR(120),
    display_name VARCHAR(120),
    avatar_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    avatar_preset VARCHAR(40),
    avatar_object_key VARCHAR(1000),
    avatar_content_type VARCHAR(100),
    avatar_size BIGINT,
    avatar_updated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_user_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_profiles_avatar_type CHECK (avatar_type IN ('NONE', 'PRESET', 'UPLOAD')),
    CONSTRAINT ck_user_profiles_avatar_preset CHECK (
        (avatar_type = 'PRESET' AND avatar_preset IN ('violet', 'blue', 'rose', 'teal'))
        OR (avatar_type <> 'PRESET' AND avatar_preset IS NULL)
    ),
    CONSTRAINT ck_user_profiles_upload_metadata CHECK (
        (avatar_type = 'UPLOAD' AND avatar_object_key IS NOT NULL AND avatar_content_type IS NOT NULL AND avatar_size IS NOT NULL)
        OR (avatar_type <> 'UPLOAD' AND avatar_object_key IS NULL AND avatar_content_type IS NULL AND avatar_size IS NULL)
    )
);
