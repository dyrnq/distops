-- ============================================================
-- V20260526 003: Blob table for layer-level size tracking
-- ============================================================

CREATE TABLE blob (
    id          BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
    inst_id     BIGINT NOT NULL COMMENT '实例 ID',
    digest      VARCHAR(255) NOT NULL COMMENT 'blob digest (sha256:xxx)',
    size        BIGINT DEFAULT NULL COMMENT '压缩大小 (bytes)',
    media_type  VARCHAR(500) DEFAULT NULL COMMENT 'MIME 类型',
    created     DATETIME(6) DEFAULT NULL COMMENT '创建时间',
    UNIQUE KEY uk_blob (inst_id, digest),
    INDEX idx_blob_inst (inst_id),
    INDEX idx_blob_digest (digest)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='镜像 layer/blob 表';

CREATE TABLE manifest_blob (
    id          BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
    manifest_id BIGINT NOT NULL COMMENT '关联的 manifest ID',
    blob_id     BIGINT NOT NULL COMMENT '关联的 blob ID',
    UNIQUE KEY uk_mb (manifest_id, blob_id),
    INDEX idx_mb_manifest (manifest_id),
    INDEX idx_mb_blob (blob_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Manifest-Blob 关联表';
