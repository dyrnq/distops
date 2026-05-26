-- ============================================================
-- V20260303 004: Add status column to blob table for GC support
-- ============================================================

ALTER TABLE blob ADD COLUMN status VARCHAR(20) DEFAULT 'active';

-- MySQL compatible:
-- ALTER TABLE blob ADD COLUMN status VARCHAR(20) DEFAULT 'active';
