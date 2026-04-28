CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    family_id   UUID NOT NULL,
    is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_refresh_tokens_hash        ON refresh_tokens(token_hash);
CREATE INDEX        idx_refresh_tokens_family_id   ON refresh_tokens(family_id);
CREATE INDEX        idx_refresh_tokens_user_active  ON refresh_tokens(user_id) WHERE is_revoked = FALSE;
