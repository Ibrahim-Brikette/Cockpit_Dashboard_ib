-- ============================================================
-- Auth: refresh_token table
-- One row = one login session. Token is never rotated.
-- ============================================================
--
-- Timeout tracking (no separate session table needed):
--   last_used_at      → updated on every /refresh → idle-timeout check (30 min)
--   original_login_at → set once at login, never changes → absolute-timeout check (8h)
--
-- OLD ROTATION DESIGN — commented, not deleted:
--   The "used" column existed when every /refresh burned the current token and
--   inserted a new row. Over an 8h session that produced 32 rows per user session.
--   Removed because the HttpOnly cookie already prevents token theft, making
--   the reuse-detection benefit of rotation negligible.
--   "used" BOOLEAN NOT NULL DEFAULT FALSE
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.refresh_token (
    id                UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    token_hash        CHAR(64)     NOT NULL,      -- SHA-256 hex digest; raw token never stored
    session_id        UUID         NOT NULL,       -- grouping key for revocation, not a FK
    user_id           UUID         NOT NULL REFERENCES cockpit.user_account(id) ON DELETE CASCADE,
    expires_at        TIMESTAMPTZ  NOT NULL,
    original_login_at TIMESTAMPTZ  NOT NULL,       -- login time; drives the 8h absolute-timeout
    last_used_at      TIMESTAMPTZ  NOT NULL,       -- updated on /refresh; drives 30-min idle-timeout
    revoked           BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_session_id ON cockpit.refresh_token (session_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id    ON cockpit.refresh_token (user_id);

-- Active-session hot path: counting and finding sessions for a user
CREATE INDEX IF NOT EXISTS idx_refresh_token_active_user
    ON cockpit.refresh_token (user_id, original_login_at)
    WHERE revoked = FALSE;
