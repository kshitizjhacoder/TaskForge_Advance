-- =============================================================================
-- V1: Master (lookup) tables.
-- Each table holds the allowed codes for one kind of master data plus display
-- information for the UI. Business tables reference these by `code` through a
-- foreign key, so an unknown code can never be stored.
-- The Java enums (Priority, TaskStatus, Role) must contain exactly these codes;
-- MasterDataConsistencyCheck verifies this when the application starts.
-- =============================================================================

CREATE TABLE master_priority (
    code          VARCHAR(30)  NOT NULL,
    label         VARCHAR(60)  NOT NULL,
    description   VARCHAR(255) NULL,
    display_order INT          NOT NULL,
    color         VARCHAR(7)   NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_master_priority PRIMARY KEY (code)
);

CREATE TABLE master_task_status (
    code          VARCHAR(30)  NOT NULL,
    label         VARCHAR(60)  NOT NULL,
    description   VARCHAR(255) NULL,
    display_order INT          NOT NULL,
    color         VARCHAR(7)   NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_master_task_status PRIMARY KEY (code)
);

CREATE TABLE master_role (
    code          VARCHAR(30)  NOT NULL,
    label         VARCHAR(60)  NOT NULL,
    description   VARCHAR(255) NULL,
    display_order INT          NOT NULL,
    color         VARCHAR(7)   NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_master_role PRIMARY KEY (code)
);

INSERT INTO master_priority (code, label, description, display_order, color) VALUES
    ('LOW',      'Low',      'Can wait; no deadline pressure',         1, '#64748B'),
    ('MEDIUM',   'Medium',   'Normal priority',                        2, '#3B82F6'),
    ('HIGH',     'High',     'Should be picked up soon',               3, '#F59E0B'),
    ('CRITICAL', 'Critical', 'Blocking work or production; do it now', 4, '#EF4444');

INSERT INTO master_task_status (code, label, description, display_order, color) VALUES
    ('TO_DO',       'To do',       'Not started yet',                  1, '#64748B'),
    ('IN_PROGRESS', 'In progress', 'Someone is working on it',         2, '#3B82F6'),
    ('BLOCKED',     'Blocked',     'Waiting on something else',        3, '#EF4444'),
    ('DONE',        'Done',        'Finished',                         4, '#22C55E');

INSERT INTO master_role (code, label, description, display_order, color) VALUES
    ('ADMIN',   'Admin',   'Full access to every project and user',      1, NULL),
    ('MANAGER', 'Manager', 'Manages projects and the people reporting to them', 2, NULL),
    ('MEMBER',  'Member',  'Works on tasks in projects they belong to',  3, NULL);
