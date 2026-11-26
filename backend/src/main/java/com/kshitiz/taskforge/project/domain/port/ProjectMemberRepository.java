package com.kshitiz.taskforge.project.domain.port;

import com.kshitiz.taskforge.project.domain.model.ProjectMember;

import java.util.List;
import java.util.UUID;

/**
 * Port (interface) for project memberships. Implemented by
 * {@code ProjectMemberPersistenceAdapter}.
 */
public interface ProjectMemberRepository {

    /**
     * Stores a new membership.
     *
     * @param member membership to store
     * @return the stored membership
     */
    ProjectMember save(ProjectMember member);

    /**
     * Checks whether a user belongs to a project.
     *
     * @param projectId project ID
     * @param userId    user ID
     * @return {@code true} if the user is a member
     */
    boolean exists(UUID projectId, UUID userId);

    /**
     * Removes a user from a project.
     *
     * @param projectId project ID
     * @param userId    user ID
     */
    void delete(UUID projectId, UUID userId);

    /**
     * Lists a project's members, earliest first.
     *
     * @param projectId project ID
     * @return memberships
     */
    List<ProjectMember> findByProjectId(UUID projectId);

    /**
     * Lists the IDs of the (non-deleted) projects a user belongs to. Used to limit task
     * searches to projects the user can see.
     *
     * @param userId user ID
     * @return project IDs
     */
    List<UUID> findProjectIdsByUserId(UUID userId);
}
