-- V3__insert_default_workspace.sql
-- Default workspace initialization

INSERT INTO workspaces (id, name, slug, description, primary_repo_url)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Default Workspace',
    'default',
    'Main workspace for local development',
    'https://github.com/organization/repo.git'
) ON CONFLICT (id) DO NOTHING;
