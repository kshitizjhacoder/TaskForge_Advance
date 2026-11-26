package com.kshitiz.taskforge.project.domain.port;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.project.domain.model.Project;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for storing and loading projects. Implemented by
 * {@code ProjectPersistenceAdapter}. Deleted projects are invisible to every method.
 */
public interface ProjectRepository {

    /**
     * Inserts a new project or updates an existing one.
     *
     * @param project project to store
     * @return the stored project with timestamps and version filled in
     */
    Project save(Project project);

    /**
     * Finds a project by ID.
     *
     * @param id project ID
     * @return the project, or empty if not found
     */
    Optional<Project> findById(UUID id);

    /**
     * Checks whether a project key is already used by a (non-deleted) project.
     *
     * @param key upper-case project key
     * @return {@code true} if taken
     */
    boolean existsByKey(String key);

    /**
     * Lists every project (for admins).
     *
     * @param pageQuery page, size and sort
     * @return one page of projects
     */
    PageResult<Project> findAll(PageQuery pageQuery);

    /**
     * Lists the projects a user is a member of.
     *
     * @param userId    the member
     * @param pageQuery page, size and sort
     * @return one page of projects
     */
    PageResult<Project> findByMember(UUID userId, PageQuery pageQuery);

    /**
     * Soft-deletes a project.
     *
     * @param id project ID
     */
    void deleteById(UUID id);
}
