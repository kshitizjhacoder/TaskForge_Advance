package com.kshitiz.taskforge.task.adapter.persistence;

import com.kshitiz.taskforge.common.adapter.persistence.BaseJpaEntity;
import com.kshitiz.taskforge.task.domain.model.Priority;
import com.kshitiz.taskforge.task.domain.model.TaskStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Database row of the {@code tasks} table, plus its tags from {@code task_tags}.
 *
 * <p>Priority and status are stored as their codes and reference the master tables.
 * Deleting a task only sets {@code deleted = true} ({@code @SoftDelete}).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tasks")
@SoftDelete(columnName = "deleted")
public class TaskJpaEntity extends BaseJpaEntity {

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "priority_code", length = 30, nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status_code", length = 30, nullable = false)
    private TaskStatus status;

    /**
     * Tags are loaded together with the task. {@code @BatchSize} loads the tags of up to 50
     * tasks in one query when a page of tasks is read, instead of one query per task.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag", length = 50, nullable = false)
    @BatchSize(size = 50)
    private List<String> tags = new ArrayList<>();
}
