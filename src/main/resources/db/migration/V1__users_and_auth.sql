CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(120) NOT NULL,
    timezone      VARCHAR(64)  NOT NULL DEFAULT 'America/Lima',
    locale        VARCHAR(10)  NOT NULL DEFAULT 'es',
    theme         VARCHAR(10)  NOT NULL DEFAULT 'system',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_theme CHECK (theme IN ('light', 'dark', 'system'))
);

CREATE TABLE refresh_token (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ,
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_expires ON refresh_token (expires_at);
