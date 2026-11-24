-- =============================================================================
-- V2: Users.
-- IDs are UUIDs stored as BINARY(16). Rows are soft-deleted (deleted = TRUE),
-- so an email stays reserved even after its user is deleted.
-- =============================================================================

CREATE TABLE users (
    id            BINARY(16)   NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role_code     VARCHAR(30)  NOT NULL,
    manager_id    BINARY(16)   NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_code) REFERENCES master_role (code),
    CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES users (id)
);

CREATE INDEX idx_users_role ON users (role_code);
CREATE INDEX idx_users_manager ON users (manager_id);
