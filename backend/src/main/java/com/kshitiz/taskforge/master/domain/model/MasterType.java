package com.kshitiz.taskforge.master.domain.model;

import com.kshitiz.taskforge.task.domain.model.Priority;
import com.kshitiz.taskforge.task.domain.model.TaskStatus;
import com.kshitiz.taskforge.user.domain.model.Role;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The kinds of master (lookup) data TaskForge has.
 *
 * <p>Each kind has a URL name used by the API ({@code /api/master/priorities}) and the Java
 * enum whose codes must match its database table. To add a new kind: create the table in a
 * Flyway migration, the JPA entity and repository, then add a constant here.
 */
public enum MasterType {
    PRIORITY("priorities", Priority.class),
    TASK_STATUS("task-statuses", TaskStatus.class),
    ROLE("roles", Role.class);

    private final String slug;
    private final Class<? extends Enum<?>> codeEnum;

    MasterType(String slug, Class<? extends Enum<?>> codeEnum) {
        this.slug = slug;
        this.codeEnum = codeEnum;
    }

    /**
     * Returns the name of this master type as used in URLs and JSON keys.
     *
     * @return URL-friendly name, e.g. "task-statuses"
     */
    public String slug() {
        return slug;
    }

    /**
     * Returns the codes the Java code expects to find in this master table.
     *
     * @return enum constant names, e.g. {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
     */
    public Set<String> expectedCodes() {
        return Arrays.stream(codeEnum.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Finds the master type for a URL name.
     *
     * @param slug URL name, e.g. "priorities"
     * @return the matching type, or empty if none matches
     */
    public static Optional<MasterType> fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(type -> type.slug.equals(slug))
                .findFirst();
    }
}
