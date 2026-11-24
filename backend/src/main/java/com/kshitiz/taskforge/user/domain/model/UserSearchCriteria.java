package com.kshitiz.taskforge.user.domain.model;

/**
 * Optional filters for searching users. A {@code null} field means "don't filter on this".
 *
 * @param name part of the full name, matched case-insensitively
 * @param role exact role
 */
public record UserSearchCriteria(String name, Role role) {
}
