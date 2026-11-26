package com.kshitiz.taskforge.task.adapter.persistence;

import com.kshitiz.taskforge.task.domain.model.TaskAssignment;
import com.kshitiz.taskforge.task.domain.port.TaskAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Database implementation of the {@link TaskAssignmentRepository} port.
 */
@Component
@RequiredArgsConstructor
public class TaskAssignmentPersistenceAdapter implements TaskAssignmentRepository {

    private final TaskAssignmentJpaRepository jpaRepository;
    private final TaskPersistenceMapper mapper;

    /** {@inheritDoc} */
    @Override
    public TaskAssignment save(TaskAssignment assignment) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(assignment)));
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(UUID taskId, UUID userId) {
        return jpaRepository.existsById(new TaskAssignmentJpaEntity.Key(taskId, userId));
    }

    /** {@inheritDoc} */
    @Override
    public void delete(UUID taskId, UUID userId) {
        jpaRepository.deleteById(new TaskAssignmentJpaEntity.Key(taskId, userId));
    }

    /** {@inheritDoc} */
    @Override
    public List<TaskAssignment> findByTaskId(UUID taskId) {
        return jpaRepository.findByTaskIdOrderByAssignedAtAsc(taskId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public int deleteByProjectAndUser(UUID projectId, UUID userId) {
        return jpaRepository.deleteByProjectAndUser(projectId, userId);
    }
}
