package com.kshitiz.taskforge.task.adapter.persistence;

import com.kshitiz.taskforge.common.adapter.persistence.PageRequests;
import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskSearchCriteria;
import com.kshitiz.taskforge.task.domain.port.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Database implementation of the {@link TaskRepository} port, using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
public class TaskPersistenceAdapter implements TaskRepository {

    /**
     * Sort names the API accepts, mapped to entity properties. Priority is not offered: its
     * codes sort alphabetically (CRITICAL, HIGH, LOW, MEDIUM), which would mislead users.
     */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "title", "title",
            "endDate", "endDate");

    private final TaskJpaRepository jpaRepository;
    private final TaskPersistenceMapper mapper;

    /**
     * {@inheritDoc}
     *
     * <p>Flushes immediately so the returned task has its final timestamps and version.
     */
    @Override
    public Task save(Task task) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(task)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Task> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    /** {@inheritDoc} */
    @Override
    public PageResult<Task> search(TaskSearchCriteria criteria, PageQuery pageQuery) {
        return PageRequests.toPageResult(
                jpaRepository.findAll(TaskSpecifications.matching(criteria),
                        PageRequests.toPageable(pageQuery, SORTABLE_FIELDS)),
                mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
