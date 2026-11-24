package com.kshitiz.taskforge.task.adapter.persistence;

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
 * Database row of the {@code task_comments} table. Soft-deleted like tasks.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task_comments")
@SoftDelete(columnName = "deleted")
public class TaskCommentJpaEntity extends BaseJpaEntity {

    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
