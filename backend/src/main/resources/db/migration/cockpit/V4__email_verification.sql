-- ============================================================
-- Email verification tokens for account activation
-- ============================================================
--
-- Created when an admin creates a new user (STANDALONE mode only).
-- The admin never sets a password — the user sets their own password
-- by clicking the link in the verification email.
--
-- Flow:
--   1. Admin POST /api/identity/users → user created with status=PENDING
--   2. One row inserted here with expires_at = now() + 24h
--   3. Raw token sent in email as ?token=<value>
--   4. User POST /api/auth/verify-email { token, password }
--   5. VerifyEmailService validates this row, sets password, status→ACTIVE
--   6. used flipped to true — link is now dead
--
-- TTL is 24h (not 5 min like password reset) because account setup
-- is not an emergency — the user may click the link the next morning.
--
-- The raw token is never stored. Only its SHA-256 hex digest (64 chars) lives here.
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.email_verification_token (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    token_hash  CHAR(64)     NOT NULL,
    user_id     UUID         NOT NULL REFERENCES cockpit.user_account(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ  NOT NULL,   -- now() + 24h at creation time
    used        BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_email_verification_token_hash UNIQUE (token_hash)
);

-- Used by VerifyEmailService lookup and by re-send checks
CREATE INDEX IF NOT EXISTS idx_email_verification_token_user_id
    ON cockpit.email_verification_token (user_id);
