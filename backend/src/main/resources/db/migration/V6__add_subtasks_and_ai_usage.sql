-- =============================================================================
-- V6: Subtasks and AI usage counting.
-- =============================================================================

-- A task may have a parent task (one level only: a subtask cannot have subtasks).
-- Subtasks always live in the same project as their parent (checked in code).
ALTER TABLE tasks ADD COLUMN parent_task_id BINARY(16) NULL AFTER project_id;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES tasks (id);
CREATE INDEX idx_tasks_parent ON tasks (parent_task_id);

-- How many AI requests each user made per day (UTC). Used to enforce the per-user
-- and app-wide daily limits that keep usage inside the Gemini free tier.
CREATE TABLE ai_usage (
    user_id    BINARY(16) NOT NULL,
    usage_date DATE       NOT NULL,
    call_count INT        NOT NULL,
    CONSTRAINT pk_ai_usage PRIMARY KEY (user_id, usage_date),
    CONSTRAINT fk_ai_usage_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_ai_usage_date ON ai_usage (usage_date);
