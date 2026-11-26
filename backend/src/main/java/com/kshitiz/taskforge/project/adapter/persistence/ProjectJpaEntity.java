package com.kshitiz.taskforge.project.adapter.persistence;

import com.kshitiz.taskforge.common.adapter.persistence.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

import java.util.UUID;

/**
 * Database row of the {@code projects} table. Deleting a project only sets
 * {@code deleted = true}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "projects")
@SoftDelete(columnName = "deleted")
public class ProjectJpaEntity extends BaseJpaEntity {

    /** Column is {@code project_key} because KEY is a reserved word in MySQL. */
    @Column(name = "project_key", length = 10, nullable = false, unique = true, updatable = false)
    private String key;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;
}
