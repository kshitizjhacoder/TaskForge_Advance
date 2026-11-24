package com.kshitiz.taskforge.task.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Database row of the {@code task_assignments} table. The primary key is the pair
 * (task_id, user_id), so a user can be assigned to a task only once.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task_assignments")
@IdClass(TaskAssignmentJpaEntity.Key.class)
public class TaskAssignmentJpaEntity {

    @Id
    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    /**
     * Composite primary key (task ID + user ID). JPA requires a separate class with the
     * same field names as the {@code @Id} fields, plus equals/hashCode.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID taskId;
        private UUID userId;

        /**
         * Creates a key.
         *
         * @param taskId task ID
         * @param userId user ID
         */
        public Key(UUID taskId, UUID userId) {
            this.taskId = taskId;
            this.userId = userId;
        }
    }
}
