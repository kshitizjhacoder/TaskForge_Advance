package com.kshitiz.taskforge.user.adapter.persistence;

import com.kshitiz.taskforge.common.adapter.persistence.Specs;
import com.kshitiz.taskforge.user.domain.model.Role;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable WHERE-clause pieces for user search. Each method returns {@code null} when its
 * filter is not set; Spring Data ignores {@code null} specifications, so filters combine
 * freely.
 */
final class UserSpecifications {

    private UserSpecifications() {
        // utility class, not meant to be instantiated
    }

    /**
     * Full name contains the text, ignoring case.
     *
     * @param name text to look for, or {@code null}
     * @return the condition, or {@code null} if no filter
     */
    static Specification<UserJpaEntity> nameContains(String name) {
        return Specs.containsIgnoreCase("fullName", name);
    }

    /**
     * User has exactly this role.
     *
     * @param role role to match, or {@code null}
     * @return the condition, or {@code null} if no filter
     */
    static Specification<UserJpaEntity> hasRole(Role role) {
        return Specs.equalTo("role", role);
    }
}
