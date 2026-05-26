-- ============================================================
-- V20260526: Add compressed_size and updated_at columns
-- ============================================================

-- SQLite: ALTER TABLE ADD COLUMN cannot have non-constant DEFAULT (no CURRENT_TIMESTAMP)
-- We add columns WITHOUT defaults; application layer handles values

-- 1. Add compressed_size to manifest
ALTER TABLE manifest ADD COLUMN compressed_size INTEGER DEFAULT NULL;

-- 2. Add updated_at to all business tables (no default — set in app code)
ALTER TABLE inst ADD COLUMN updated_at DATETIME DEFAULT NULL;
ALTER TABLE repo ADD COLUMN updated_at DATETIME DEFAULT NULL;
ALTER TABLE manifest ADD COLUMN updated_at DATETIME DEFAULT NULL;
ALTER TABLE artifact ADD COLUMN updated_at DATETIME DEFAULT NULL;

-- 3. Recreate artifact_manifest_oci_view with compressed_size
DROP VIEW IF EXISTS artifact_manifest_oci_view;
CREATE VIEW artifact_manifest_oci_view AS
SELECT
    a.id,
    a.tag_name,
    a.full_name,
    a.repo_id,
    a.repo_name,
    a.inst_id,
    m.id AS manifest_list_id,
    m.digest AS manifest_list_digest,
    m.media_type AS parent_media_type,
    m.size AS manifest_list_size,
    m.compressed_size AS manifest_list_compressed_size,
    m.created AS manifest_list_created,
    child.id AS child_manifest_id,
    child.digest AS child_digest,
    child.os_arch,
    child.os,
    child.variant,
    child.size AS child_size,
    child.media_type AS child_media_type,
    child.os_version,
    child.features,
    child.created AS child_created,
    child.annotations,
    child.config_digest
FROM artifact a
         JOIN manifest m ON a.manifest_id = m.id
         JOIN manifest child ON m.digest = child.parent_digest
WHERE m.media_type IN (
    'application/vnd.oci.image.index.v1+json','application/vnd.docker.distribution.manifest.list.v2+json'
);
