-- V2: Phase 3 schema additions
-- New columns for RepoSnapshot (tree hash, file manifest, snapshot timing)
-- New columns for ExtractionRun (workspace link, typed status)

-- RepoSnapshot additions
ALTER TABLE repo_snapshots
    ADD COLUMN IF NOT EXISTS tree_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS file_manifest JSONB,
    ADD COLUMN IF NOT EXISTS total_files INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS snapshot_taken_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_repo_snapshots_tree_hash ON repo_snapshots(tree_hash);
CREATE INDEX IF NOT EXISTS idx_repo_snapshots_workspace ON repo_snapshots(workspace_id);

-- ExtractionRun additions
ALTER TABLE extraction_runs
    ADD COLUMN IF NOT EXISTS workspace_id UUID REFERENCES workspaces(id),
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ;

-- Rename started_at from created_at if needed (only if column was created_at previously)
-- Note: created_at is preserved separately via @CreationTimestamp

CREATE INDEX IF NOT EXISTS idx_extraction_runs_workspace ON extraction_runs(workspace_id);

-- ProductMemoryItem: ensure grounding_ref column exists for idempotency checks
ALTER TABLE product_memory_items
    ADD COLUMN IF NOT EXISTS grounding_ref VARCHAR(1024);

CREATE INDEX IF NOT EXISTS idx_pmi_grounding_ref ON product_memory_items(grounding_ref)
    WHERE grounding_ref IS NOT NULL;
