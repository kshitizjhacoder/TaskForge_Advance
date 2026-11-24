package com.kshitiz.taskforge.task.adapter.persistence;

import com.kshitiz.taskforge.task.domain.model.TaskComment;
import com.kshitiz.taskforge.task.domain.port.TaskCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Database implementation of the {@link TaskCommentRepository} port.
 */
@Component
@RequiredArgsConstructor
public class TaskCommentPersistenceAdapter implements TaskCommentRepository {

    private final TaskCommentJpaRepository jpaRepository;
    private final TaskPersistenceMapper mapper;

    /**
     * {@inheritDoc}
     *
     * <p>Flushes immediately so the returned comment has its final timestamps and version.
     */
    @Override
    public TaskComment save(TaskComment comment) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(comment)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<TaskComment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<TaskComment> findByTaskId(UUID taskId) {
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
