-- =============================================================================
-- V5: File attachments on tasks.
-- Only metadata is stored here; the file bytes live in S3-compatible storage
-- under storage_key. Deleting an attachment removes the row and the object.
-- =============================================================================

CREATE TABLE task_attachments (
    id           BINARY(16)   NOT NULL,
    task_id      BINARY(16)   NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    storage_key  VARCHAR(300) NOT NULL,
    uploaded_by  BINARY(16)   NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    version      BIGINT       NOT NULL,
    CONSTRAINT pk_task_attachments PRIMARY KEY (id),
    CONSTRAINT uk_task_attachments_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_task_attachments_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_task_attachments_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id)
);

CREATE INDEX idx_task_attachments_task ON task_attachments (task_id);
