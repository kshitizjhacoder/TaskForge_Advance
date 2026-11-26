package com.kshitiz.taskforge.task.adapter.persistence;

import com.kshitiz.taskforge.common.adapter.persistence.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Database row of the {@code task_attachments} table (metadata only; the bytes are in file
 * storage). Attachments are deleted for real, not soft-deleted, because the stored file is
 * removed too.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task_attachments")
public class TaskAttachmentJpaEntity extends BaseJpaEntity {

    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "file_name", length = 255, nullable = false, updatable = false)
    private String fileName;

    @Column(name = "content_type", length = 100, nullable = false, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "storage_key", length = 300, nullable = false, unique = true, updatable = false)
    private String storageKey;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;
}
