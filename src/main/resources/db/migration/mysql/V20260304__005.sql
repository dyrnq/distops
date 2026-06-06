-- ============================================================
-- V20260304 005: Refresh token revocation blacklist (JTI store)
-- ============================================================

CREATE TABLE IF NOT EXISTS refresh_token_revocation (
    jti VARCHAR(64) NOT NULL COMMENT 'JWT ID of the revoked token',
    inst_id BIGINT NOT NULL COMMENT 'registry instance id',
    username VARCHAR(256) NOT NULL COMMENT 'which user''s token was revoked',
    revoked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL COMMENT 'original token expiry (for cleanup)',
    PRIMARY KEY (jti, inst_id),
    INDEX idx_revocation_username (username),
    INDEX idx_revocation_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
