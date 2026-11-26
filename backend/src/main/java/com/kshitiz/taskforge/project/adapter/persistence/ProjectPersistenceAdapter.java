package com.kshitiz.taskforge.project.adapter.persistence;

import com.kshitiz.taskforge.common.adapter.persistence.PageRequests;
import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.project.domain.model.Project;
import com.kshitiz.taskforge.project.domain.port.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Database implementation of the {@link ProjectRepository} port.
 */
@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectRepository {

    /** Sort names the API accepts, mapped to entity properties. */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "name", "name",
            "key", "key",
            "createdAt", "createdAt");

    private final ProjectJpaRepository jpaRepository;
    private final ProjectPersistenceMapper mapper;

    /**
     * {@inheritDoc}
     *
     * <p>Flushes immediately so the returned project has its final timestamps and version.
     */
    @Override
    public Project save(Project project) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(project)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Project> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByKey(String key) {
        return jpaRepository.existsByKey(key);
    }

    /** {@inheritDoc} */
    @Override
    public PageResult<Project> findAll(PageQuery pageQuery) {
        return PageRequests.toPageResult(
                jpaRepository.findAll(PageRequests.toPageable(pageQuery, SORTABLE_FIELDS)), mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public PageResult<Project> findByMember(UUID userId, PageQuery pageQuery) {
        return PageRequests.toPageResult(
                jpaRepository.findByMember(userId, PageRequests.toPageable(pageQuery, SORTABLE_FIELDS)),
                mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
