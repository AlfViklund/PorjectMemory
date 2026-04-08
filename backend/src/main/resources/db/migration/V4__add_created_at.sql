-- V4__add_created_at.sql
-- Add missing created_at column to extraction_runs

ALTER TABLE extraction_runs
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
