package com.kshitiz.taskforge.user.domain.model;

/**
 * What a user is allowed to do across the application.
 *
 * <p>The codes must match the rows of the {@code master_role} table. Permission checks
 * based on these roles are added in Phase 2.
 */
public enum Role {
    ADMIN,
    MANAGER,
    MEMBER
}
