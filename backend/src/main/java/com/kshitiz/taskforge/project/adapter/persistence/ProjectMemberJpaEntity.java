package com.kshitiz.taskforge.project.adapter.persistence;

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
 * Database row of the {@code project_members} table. The primary key is the pair
 * (project_id, user_id), so a user can join a project only once.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "project_members")
@IdClass(ProjectMemberJpaEntity.Key.class)
public class ProjectMemberJpaEntity {

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    /**
     * Composite primary key (project ID + user ID), required by {@code @IdClass}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID projectId;
        private UUID userId;

        /**
         * Creates a key.
         *
         * @param projectId project ID
         * @param userId    user ID
         */
        public Key(UUID projectId, UUID userId) {
            this.projectId = projectId;
            this.userId = userId;
        }
    }
}
