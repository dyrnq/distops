-- ============================================================
-- V20260304 005: Refresh token revocation blacklist (JTI store)
-- ============================================================

CREATE TABLE IF NOT EXISTS refresh_token_revocation (
    jti VARCHAR(64) NOT NULL,
    inst_id BIGINT NOT NULL,
    username VARCHAR(256) NOT NULL,
    revoked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    PRIMARY KEY (jti, inst_id)
);

CREATE INDEX idx_revocation_username ON refresh_token_revocation (username);
CREATE INDEX idx_revocation_expires ON refresh_token_revocation (expires_at);
