-- =============================================================================
-- V3: Tasks, task tags, task assignments and task comments.
-- project_id has no foreign key yet; the projects table arrives in Phase 3.
-- =============================================================================

CREATE TABLE tasks (
    id            BINARY(16)   NOT NULL,
    project_id    BINARY(16)   NULL,
    title         VARCHAR(200) NOT NULL,
    description   TEXT         NULL,
    created_by    BINARY(16)   NOT NULL,
    start_date    DATE         NULL,
    end_date      DATE         NULL,
    priority_code VARCHAR(30)  NOT NULL,
    status_code   VARCHAR(30)  NOT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL,
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_tasks_priority FOREIGN KEY (priority_code) REFERENCES master_priority (code),
    CONSTRAINT fk_tasks_status FOREIGN KEY (status_code) REFERENCES master_task_status (code),
    CONSTRAINT ck_tasks_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_tasks_project ON tasks (project_id);
CREATE INDEX idx_tasks_status ON tasks (status_code);
CREATE INDEX idx_tasks_priority ON tasks (priority_code);
CREATE INDEX idx_tasks_created_by ON tasks (created_by);
CREATE INDEX idx_tasks_end_date ON tasks (end_date);

-- Free-form labels such as "backend" or "urgent"; one row per tag.
CREATE TABLE task_tags (
    task_id BINARY(16)  NOT NULL,
    tag     VARCHAR(50) NOT NULL,
    CONSTRAINT pk_task_tags PRIMARY KEY (task_id, tag),
    CONSTRAINT fk_task_tags_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE
);

-- Which users are assigned to which tasks. Removing an assignment deletes the row.
CREATE TABLE task_assignments (
    task_id     BINARY(16)  NOT NULL,
    user_id     BINARY(16)  NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_task_assignments PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_task_assignments_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignments_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_task_assignments_user ON task_assignments (user_id);

CREATE TABLE task_comments (
    id         BINARY(16)  NOT NULL,
    task_id    BINARY(16)  NOT NULL,
    author_id  BINARY(16)  NOT NULL,
    content    TEXT        NOT NULL,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version    BIGINT      NOT NULL,
    CONSTRAINT pk_task_comments PRIMARY KEY (id),
    CONSTRAINT fk_task_comments_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_task_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_task_comments_task ON task_comments (task_id);
