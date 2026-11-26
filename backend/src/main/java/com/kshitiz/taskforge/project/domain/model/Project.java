package com.kshitiz.taskforge.project.domain.model;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A project groups tasks and the people who work on them. It has one owner (who is also a
 * member) and any number of other members.
 *
 * <p>Rules:
 * <ul>
 *   <li>the key is 2–10 characters: an uppercase letter followed by uppercase letters or
 *       digits (e.g. "TF", "WEB2"); it is unique and never changes;</li>
 *   <li>the name is required, max {@value #MAX_NAME_LENGTH} characters.</li>
 * </ul>
 * New projects are made with {@link #create}; projects loaded from the database are rebuilt
 * with {@code Project.restore()...build()}.
 */
@Getter
@Builder(builderMethodName = "restore")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Project {

    public static final int MAX_NAME_LENGTH = 100;
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Z][A-Z0-9]{1,9}");

    private final UUID id;
    /** Short, human-friendly identifier such as "TF". */
    private final String key;
    private String name;
    private String description;
    private final UUID ownerId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Long version;

    /**
     * Creates a new project. The key is upper-cased, so "tf" becomes "TF".
     *
     * @param key         short identifier
     * @param name        display name
     * @param description details (optional)
     * @param ownerId     user who owns the project
     * @return the new, not yet saved, project
     * @throws BusinessRuleViolationException if the key or name is invalid
     */
    public static Project create(String key, String name, String description, UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        return new Project(UUID.randomUUID(), validateKey(key), validateName(name), trimToNull(description),
                ownerId, null, null, null);
    }

    /**
     * Replaces the project's name and description. The key cannot change.
     *
     * @param name        new name
     * @param description new description (optional)
     * @throws BusinessRuleViolationException if the name is invalid
     */
    public void updateDetails(String name, String description) {
        this.name = validateName(name);
        this.description = trimToNull(description);
    }

    /**
     * Checks whether a user owns this project.
     *
     * @param userId user to check
     * @return {@code true} if the user is the owner
     */
    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }

    /**
     * Checks and normalises a project key.
     *
     * @param key raw key
     * @return trimmed, upper-case key
     */
    private static String validateKey(String key) {
        String normalized = key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
        if (!KEY_PATTERN.matcher(normalized).matches()) {
            throw new BusinessRuleViolationException(
                    "Project key must be 2-10 characters: a letter followed by letters or digits (e.g. TF, WEB2)");
        }
        return normalized;
    }

    /**
     * Checks and trims a project name.
     *
     * @param name raw name
     * @return trimmed name
     */
    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleViolationException("Project name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleViolationException("Project name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    /**
     * Trims text and turns blank text into {@code null}.
     *
     * @param text raw text
     * @return trimmed text, or {@code null} if blank
     */
    private static String trimToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
