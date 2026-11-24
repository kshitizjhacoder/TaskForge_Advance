package com.kshitiz.taskforge.user.domain.model;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * A person who can log in and work on tasks.
 *
 * <p>Plain Java: no database or web code. Two ways to get an instance:
 * <ul>
 *   <li>{@link #register} – a brand-new user; applies the business rules;</li>
 *   <li>{@code User.restore()...build()} – rebuild an existing user loaded from the
 *       database (used only by the persistence mapper).</li>
 * </ul>
 * The password is never stored here in plain text, only its BCrypt hash.
 */
@Getter
@Builder(builderMethodName = "restore")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private final UUID id;
    private String fullName;
    private final String email;
    private String passwordHash;
    private Role role;
    /** The user's manager, or {@code null} if they report to nobody. */
    private UUID managerId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Long version;

    /**
     * Creates a new user with a fresh ID. The email is trimmed and lower-cased so
     * "Ravi@Mail.com" and "ravi@mail.com" count as the same address.
     *
     * @param fullName     display name
     * @param email        login email
     * @param passwordHash BCrypt hash of the password (hashing happens in the service)
     * @param role         application role
     * @return the new, not yet saved, user
     * @throws BusinessRuleViolationException if a required value is missing
     */
    public static User register(String fullName, String email, String passwordHash, Role role) {
        requireText(fullName, "Full name");
        requireText(email, "Email");
        requireText(passwordHash, "Password");
        if (role == null) {
            throw new BusinessRuleViolationException("Role is required");
        }
        return new User(UUID.randomUUID(), fullName.trim(), normalizeEmail(email), passwordHash, role,
                null, null, null, null);
    }

    /**
     * Makes {@code manager} this user's manager.
     *
     * @param manager the user who will manage this user
     * @throws BusinessRuleViolationException if the user would manage themselves
     */
    public void assignManager(User manager) {
        Objects.requireNonNull(manager, "manager");
        if (manager.getId().equals(id)) {
            throw new BusinessRuleViolationException("A user cannot be their own manager");
        }
        this.managerId = manager.getId();
    }

    /**
     * Removes the user's manager, so they report to nobody.
     */
    public void removeManager() {
        this.managerId = null;
    }

    /**
     * Converts an email to the stored form: trimmed and lower-case. Use it before every
     * lookup by email so searches match regardless of capitalisation.
     *
     * @param email raw email
     * @return normalised email, or {@code null} if {@code email} is {@code null}
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Throws if a required text value is missing or blank.
     *
     * @param value     value to check
     * @param fieldName name used in the error message
     */
    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(fieldName + " is required");
        }
    }
}
