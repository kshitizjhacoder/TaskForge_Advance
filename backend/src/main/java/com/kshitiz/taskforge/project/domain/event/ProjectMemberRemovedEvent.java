package com.kshitiz.taskforge.project.domain.event;

import java.util.UUID;

/**
 * Published when a user is removed from a project.
 *
 * <p>Other features react to it without the project feature knowing about them: the task
 * feature removes the user's assignments on that project's tasks. This keeps the dependency
 * one-way (task depends on project, never the reverse).
 *
 * @param projectId the project
 * @param userId    the removed user
 */
public record ProjectMemberRemovedEvent(UUID projectId, UUID userId) {
}
