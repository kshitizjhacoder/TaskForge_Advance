-- =============================================================================
-- V4: Projects and project membership. Every task now belongs to a project.
-- =============================================================================

CREATE TABLE projects (
    id          BINARY(16)   NOT NULL,
    project_key VARCHAR(10)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    owner_id    BINARY(16)   NOT NULL,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    version     BIGINT       NOT NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    -- A key stays reserved even after its project is soft-deleted
    CONSTRAINT uk_projects_key UNIQUE (project_key),
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_projects_owner ON projects (owner_id);

-- Who belongs to which project. The owner always has a row here too.
CREATE TABLE project_members (
    project_id BINARY(16)  NOT NULL,
    user_id    BINARY(16)  NOT NULL,
    joined_at  DATETIME(6) NOT NULL,
    CONSTRAINT pk_project_members PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_members_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_project_members_user ON project_members (user_id);

-- Tasks must now belong to a project.
-- This fails if a database still holds tasks without a project (created before
-- Phase 3); delete or move those rows first. No deployed environment has any.
ALTER TABLE tasks MODIFY project_id BINARY(16) NOT NULL;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id);
