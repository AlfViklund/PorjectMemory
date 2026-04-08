-- V1__init_core_schema.sql
-- Product Memory OS — Initial Core Schema
-- All tables for the full domain model

-- Extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ================== workspaces ==================
CREATE TABLE workspaces (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    primary_repo_url VARCHAR(512),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ================== source_assets ==================
CREATE TABLE source_assets (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    file_name       VARCHAR(512) NOT NULL,
    asset_type      VARCHAR(50)  NOT NULL,
    storage_key     VARCHAR(1024) NOT NULL,
    size_bytes      BIGINT,
    content_hash    VARCHAR(64),
    mime_type       VARCHAR(100),
    raw_text        TEXT,
    metadata        JSONB,
    processed       BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_source_assets_workspace ON source_assets(workspace_id);

-- ================== source_sections ==================
CREATE TABLE source_sections (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    source_asset_id UUID        NOT NULL REFERENCES source_assets(id) ON DELETE CASCADE,
    title           VARCHAR(512),
    content         TEXT        NOT NULL,
    section_index   INT         NOT NULL,
    heading_level   INT,
    start_offset    BIGINT,
    end_offset      BIGINT
);
CREATE INDEX idx_source_sections_asset ON source_sections(source_asset_id);

-- ================== change_requests ==================
CREATE TABLE change_requests (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    short_id        VARCHAR(20)  NOT NULL UNIQUE,
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    title           VARCHAR(512) NOT NULL,
    intent          TEXT        NOT NULL,
    rationale       TEXT,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PROPOSED',
    priority        INT          NOT NULL DEFAULT 3,
    metadata        JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    merged_at       TIMESTAMPTZ,
    CONSTRAINT cr_status_check CHECK (status IN (
        'PROPOSED','IMPACT_ANALYSIS_PENDING','IMPACT_ANALYSIS_COMPLETE',
        'IN_PLANNING','PLANNED','EXECUTING','AWAITING_REVIEW',
        'APPROVED','MERGED','REJECTED','CANCELLED'
    ))
);
CREATE INDEX idx_cr_workspace ON change_requests(workspace_id);
CREATE INDEX idx_cr_status ON change_requests(status);

-- ================== requirements ==================
CREATE TABLE requirements (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id   UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    source_section_id   UUID        REFERENCES source_sections(id) ON DELETE SET NULL,
    title               VARCHAR(512) NOT NULL,
    description         TEXT,
    grounding_quote     TEXT,
    confidence          DOUBLE PRECISION,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_requirements_cr ON requirements(change_request_id);

-- ================== product_memory_items ==================
CREATE TABLE product_memory_items (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id        UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    item_type           VARCHAR(50)  NOT NULL,
    name                VARCHAR(512) NOT NULL,
    description         TEXT,
    status              VARCHAR(30)  NOT NULL DEFAULT 'PROPOSED',
    confidence          DOUBLE PRECISION DEFAULT 0.5,
    source_type         VARCHAR(50),
    extraction_run_id   UUID,
    source_section_id   UUID        REFERENCES source_sections(id) ON DELETE SET NULL,
    grounding_ref       VARCHAR(1024),
    properties          JSONB,
    stale_reason        TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pmi_type_check CHECK (item_type IN (
        'CAPABILITY','SCREEN','API_ENDPOINT','DATA_ENTITY',
        'DECISION','REQUIREMENT','WORKFLOW','INTEGRATION','BUSINESS_RULE'
    )),
    CONSTRAINT pmi_status_check CHECK (status IN (
        'FRESH','STALE','PROPOSED','SUPERSEDED','ARCHIVED'
    ))
);
CREATE INDEX idx_pmi_workspace_type ON product_memory_items(workspace_id, item_type);
CREATE INDEX idx_pmi_status ON product_memory_items(status);
CREATE INDEX idx_pmi_properties ON product_memory_items USING gin(properties);

-- ================== memory_links (graph edges) ==================
CREATE TABLE memory_links (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    source_item_id  UUID        NOT NULL REFERENCES product_memory_items(id) ON DELETE CASCADE,
    target_item_id  UUID        NOT NULL REFERENCES product_memory_items(id) ON DELETE CASCADE,
    link_type       VARCHAR(60)  NOT NULL,
    confidence      DOUBLE PRECISION DEFAULT 1.0,
    stale           BOOLEAN      NOT NULL DEFAULT FALSE,
    description     TEXT,
    properties      JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ml_source ON memory_links(source_item_id);
CREATE INDEX idx_ml_target ON memory_links(target_item_id);
CREATE INDEX idx_ml_type ON memory_links(link_type);

-- ================== repo_snapshots ==================
CREATE TABLE repo_snapshots (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id        UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    repo_url            VARCHAR(512) NOT NULL,
    branch              VARCHAR(255),
    commit_sha          VARCHAR(40),
    tree_sha            VARCHAR(40),
    dirty_state         BOOLEAN      NOT NULL DEFAULT FALSE,
    batch_content_hash  VARCHAR(64),
    metadata            JSONB,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_repo_snapshots_workspace ON repo_snapshots(workspace_id);

-- ================== extraction_runs ==================
CREATE TABLE extraction_runs (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_snapshot_id    UUID        NOT NULL REFERENCES repo_snapshots(id) ON DELETE CASCADE,
    extractor_version   VARCHAR(50)  NOT NULL,
    extractor_category  VARCHAR(50)  NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    items_extracted     INT,
    config              JSONB,
    result_summary      JSONB,
    error_message       TEXT,
    started_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_extraction_runs_snapshot ON extraction_runs(repo_snapshot_id);

-- ================== impact_analysis_outputs ==================
CREATE TABLE impact_analysis_outputs (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    short_id            VARCHAR(20)  NOT NULL UNIQUE,
    change_request_id   UUID        NOT NULL UNIQUE REFERENCES change_requests(id) ON DELETE CASCADE,
    schema_version      VARCHAR(20)  NOT NULL DEFAULT '1.0',
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payload             JSONB        NOT NULL DEFAULT '{}',
    error_message       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    CONSTRAINT iao_status_check CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);
CREATE INDEX idx_iao_cr ON impact_analysis_outputs(change_request_id);
CREATE INDEX idx_iao_payload ON impact_analysis_outputs USING gin(payload);

-- ================== tasks ==================
CREATE TABLE tasks (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    short_id                    VARCHAR(20)  NOT NULL UNIQUE,
    change_request_id           UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    title                       VARCHAR(512) NOT NULL,
    rationale                   TEXT,
    status                      VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    context_pack_validated_at   TIMESTAMPTZ,
    ordinal                     INT         NOT NULL DEFAULT 1,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT task_status_check CHECK (status IN (
        'PENDING','AWAITING_CONTEXT_PACK','READY','EXECUTING','COMPLETED','FAILED','CANCELLED'
    ))
);
CREATE INDEX idx_tasks_cr ON tasks(change_request_id);
CREATE INDEX idx_tasks_status ON tasks(status);

-- ================== task_context_packs ==================
CREATE TABLE task_context_packs (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    short_id                    VARCHAR(20)  NOT NULL UNIQUE,
    task_id                     UUID        NOT NULL UNIQUE REFERENCES tasks(id) ON DELETE CASCADE,
    schema_version              VARCHAR(20)  NOT NULL DEFAULT '1.0',
    validation_status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    has_unresolved_conflicts    BOOLEAN      NOT NULL DEFAULT FALSE,
    payload                     JSONB        NOT NULL DEFAULT '{}',
    extensions                  JSONB,
    source_precedence           VARCHAR(50),
    conflict_details            TEXT,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    validated_at                TIMESTAMPTZ,
    CONSTRAINT tcp_validation_check CHECK (validation_status IN ('PENDING','VALID','INVALID','CONFLICT'))
);
CREATE INDEX idx_tcp_task ON task_context_packs(task_id);
CREATE INDEX idx_tcp_payload ON task_context_packs USING gin(payload);

-- ================== reviews ==================
CREATE TABLE reviews (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    short_id            VARCHAR(20)  NOT NULL UNIQUE,
    change_request_id   UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    reviewer_id         VARCHAR(255),
    reviewer_comment    TEXT,
    approval_details    JSONB,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    decided_at          TIMESTAMPTZ,
    CONSTRAINT review_status_check CHECK (status IN (
        'PENDING','IN_PROGRESS','APPROVED','PARTIALLY_APPROVED','REJECTED'
    ))
);
CREATE INDEX idx_reviews_cr ON reviews(change_request_id);
CREATE INDEX idx_reviews_status ON reviews(status);

-- ================== memory_merge_plans ==================
CREATE TABLE memory_merge_plans (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    short_id            VARCHAR(20)  NOT NULL UNIQUE,
    review_id           UUID        NOT NULL UNIQUE REFERENCES reviews(id) ON DELETE CASCADE,
    schema_version      VARCHAR(20)  NOT NULL DEFAULT '1.0',
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING_REVIEW',
    payload             JSONB        NOT NULL DEFAULT '{}',
    approval_decisions  JSONB,
    suggested_follow_ups JSONB,
    reviewer_id         VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    approved_at         TIMESTAMPTZ,
    applied_at          TIMESTAMPTZ,
    application_error   TEXT,
    CONSTRAINT mmp_status_check CHECK (status IN (
        'PENDING_REVIEW','PARTIALLY_APPROVED','APPROVED','APPLIED','REJECTED','FAILED'
    ))
);
CREATE INDEX idx_mmp_review ON memory_merge_plans(review_id);

-- ================== execution_runs ==================
CREATE TABLE execution_runs (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id             UUID        NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    task_context_pack_id UUID       REFERENCES task_context_packs(id) ON DELETE SET NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'QUEUED',
    docker_image        VARCHAR(512),
    container_id        VARCHAR(128),
    stdout_log          TEXT,
    stderr_log          TEXT,
    exit_code           INT,
    runtime_metadata    JSONB,
    started_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT er_status_check CHECK (status IN (
        'QUEUED','INITIALIZING','RUNNING','COMPLETED','FAILED','TIMED_OUT','CANCELLED'
    ))
);
CREATE INDEX idx_er_task ON execution_runs(task_id);
CREATE INDEX idx_er_status ON execution_runs(status);

-- ================== evidences ==================
CREATE TABLE evidences (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_run_id            UUID        NOT NULL REFERENCES execution_runs(id) ON DELETE CASCADE,
    evidence_type               VARCHAR(50)  NOT NULL,
    title                       VARCHAR(512),
    description                 TEXT,
    acceptance_criterion_id     VARCHAR(100),
    outcome                     VARCHAR(20),
    storage_key                 VARCHAR(1024),
    content                     TEXT,
    payload                     JSONB,
    invalid                     BOOLEAN      NOT NULL DEFAULT FALSE,
    invalid_reason              VARCHAR(512),
    collected_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_evidences_run ON evidences(execution_run_id);

-- ================== findings ==================
CREATE TABLE findings (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id   UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    execution_run_id    UUID        REFERENCES execution_runs(id) ON DELETE SET NULL,
    memory_item_id      UUID        REFERENCES product_memory_items(id) ON DELETE SET NULL,
    title               VARCHAR(512) NOT NULL,
    description         TEXT        NOT NULL,
    severity            VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    finding_type        VARCHAR(50),
    suggested_follow_up TEXT,
    acknowledged        BOOLEAN      NOT NULL DEFAULT FALSE,
    details             JSONB,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT finding_severity_check CHECK (severity IN (
        'INFO','LOW','MEDIUM','HIGH','CRITICAL'
    ))
);
CREATE INDEX idx_findings_cr ON findings(change_request_id);
CREATE INDEX idx_findings_severity ON findings(severity);
