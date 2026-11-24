package com.kshitiz.taskforge.common.security;

import com.kshitiz.taskforge.user.domain.model.Role;

import java.util.Objects;
import java.util.UUID;

/**
 * The logged-in user making the current request, as read from their access token.
 *
 * <p>Controllers receive it with {@code @AuthenticationPrincipal CurrentUser actor} and pass
 * it to services, which use it for "who did this" (e.g. task creator) and permission checks.
 * Passing it explicitly (instead of reading a global security context inside services)
 * keeps services easy to unit-test.
 *
 * @param id    user ID
 * @param email login email
 * @param role  application role
 */
public record CurrentUser(UUID id, String email, Role role) {

    /**
     * Validates that every value is present.
     */
    public CurrentUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
    }

    /**
     * Checks whether the user is an administrator.
     *
     * @return {@code true} for ADMIN
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Checks whether the user is a manager or administrator, i.e. may manage other people's
     * tasks.
     *
     * @return {@code true} for MANAGER and ADMIN
     */
    public boolean isManagerOrAdmin() {
        return role == Role.MANAGER || role == Role.ADMIN;
    }

    /**
     * Checks whether this user is the given user.
     *
     * @param userId ID to compare with
     * @return {@code true} if it is the same user
     */
    public boolean is(UUID userId) {
        return id.equals(userId);
    }
}
