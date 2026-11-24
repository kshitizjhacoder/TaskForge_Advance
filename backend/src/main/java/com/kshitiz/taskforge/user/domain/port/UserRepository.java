package com.kshitiz.taskforge.user.domain.port;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.user.domain.model.Role;
import com.kshitiz.taskforge.user.domain.model.User;
import com.kshitiz.taskforge.user.domain.model.UserSearchCriteria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for storing and loading users. Implemented by
 * {@code UserPersistenceAdapter}. Deleted users are invisible to every method.
 */
public interface UserRepository {

    /**
     * Inserts a new user or updates an existing one.
     *
     * @param user user to store
     * @return the stored user with timestamps and version filled in
     */
    User save(User user);

    /**
     * Finds a user by ID.
     *
     * @param id user ID
     * @return the user, or empty if not found
     */
    Optional<User> findById(UUID id);

    /**
     * Finds a user by email (used at login).
     *
     * @param email normalised email
     * @return the user, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Counts users with a role. Used to make sure at least one admin always remains.
     *
     * @param role role to count
     * @return number of (non-deleted) users with that role
     */
    long countByRole(Role role);

    /**
     * Checks whether an email is already taken.
     *
     * @param email normalised email
     * @return {@code true} if a user has this email
     */
    boolean existsByEmail(String email);

    /**
     * Finds one page of users matching the filters.
     *
     * @param criteria  optional filters
     * @param pageQuery page, size and sort
     * @return matching users
     */
    PageResult<User> search(UserSearchCriteria criteria, PageQuery pageQuery);

    /**
     * Lists the users who report directly to a manager.
     *
     * @param managerId the manager's ID
     * @return direct reports, sorted by name
     */
    List<User> findByManagerId(UUID managerId);

    /**
     * Removes the manager from everyone who reports to {@code managerId}. Used before
     * deleting a manager so nobody points to a deleted user.
     *
     * @param managerId the manager's ID
     */
    void clearManager(UUID managerId);

    /**
     * Soft-deletes a user (the row is kept but hidden).
     *
     * @param id user ID
     */
    void deleteById(UUID id);
}
