-- ============================================================
-- Brute-force protection + password reset tokens
-- ============================================================
--
-- failed_login_attempts  incremented on each wrong password; reset to 0 on lockout trigger or success
-- locked_until           set on timed lockout; null means not brute-force locked
-- lockout_count          how many times this account has been locked — survives auto-expiry
--                        attacker cannot erase it by waiting; only a successful reset clears it
-- is_permanently_locked  true after 4th lockout; no auto-expiry; only password reset clears it
-- ============================================================

ALTER TABLE cockpit.user_account
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until           TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS lockout_count          INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_permanently_locked  BOOLEAN     NOT NULL DEFAULT FALSE;

-- ============================================================
-- Shared by both flows:
--   - Lockout reset  : token created by BruteForceGuard on lockout
--   - Forgot password: token created by ForgotPasswordUseCase on user request
-- The raw token is never stored — only its SHA-256 hex digest lives here.
-- expires_at = now() + 5 min. used flips to true after first successful consumption.
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.password_reset_token (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    token_hash  CHAR(64)     NOT NULL,
    user_id     UUID         NOT NULL REFERENCES cockpit.user_account(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_user_id
    ON cockpit.password_reset_token (user_id);

-- Hot path: ForgotPasswordUseCase checks whether a valid token already exists
-- before creating a new one (prevents email bombing).
CREATE INDEX IF NOT EXISTS idx_password_reset_token_active
    ON cockpit.password_reset_token (user_id, expires_at)
    WHERE used = FALSE;
