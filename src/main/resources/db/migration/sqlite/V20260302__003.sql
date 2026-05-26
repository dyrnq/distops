-- ============================================================
-- V20260526 003: Blob table for layer-level size tracking
-- ============================================================

CREATE TABLE blob (
    id          INTEGER NOT NULL PRIMARY KEY,
    inst_id     INTEGER NOT NULL,
    digest      VARCHAR(255) NOT NULL,
    size        INTEGER DEFAULT NULL,
    media_type  VARCHAR(500) DEFAULT NULL,
    created     DATETIME DEFAULT NULL,
    UNIQUE (inst_id, digest)
);

CREATE INDEX idx_blob_inst ON blob (inst_id);
CREATE INDEX idx_blob_digest ON blob (digest);

-- Manifest <-> Blob association (many-to-many)
CREATE TABLE manifest_blob (
    id          INTEGER NOT NULL PRIMARY KEY,
    manifest_id INTEGER NOT NULL,
    blob_id     INTEGER NOT NULL,
    UNIQUE (manifest_id, blob_id)
);

CREATE INDEX idx_mb_manifest ON manifest_blob (manifest_id);
CREATE INDEX idx_mb_blob ON manifest_blob (blob_id);
