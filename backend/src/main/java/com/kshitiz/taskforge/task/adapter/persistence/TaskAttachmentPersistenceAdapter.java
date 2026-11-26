package com.kshitiz.taskforge.task.adapter.persistence;

import com.kshitiz.taskforge.task.domain.model.TaskAttachment;
import com.kshitiz.taskforge.task.domain.port.TaskAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Database implementation of the {@link TaskAttachmentRepository} port.
 */
@Component
@RequiredArgsConstructor
public class TaskAttachmentPersistenceAdapter implements TaskAttachmentRepository {

    private final TaskAttachmentJpaRepository jpaRepository;
    private final TaskPersistenceMapper mapper;

    /** {@inheritDoc} */
    @Override
    public TaskAttachment save(TaskAttachment attachment) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(attachment)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<TaskAttachment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<TaskAttachment> findByTaskId(UUID taskId) {
        return jpaRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
