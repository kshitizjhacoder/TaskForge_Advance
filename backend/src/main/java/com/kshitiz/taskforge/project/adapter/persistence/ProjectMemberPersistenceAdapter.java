package com.kshitiz.taskforge.project.adapter.persistence;

import com.kshitiz.taskforge.project.domain.model.ProjectMember;
import com.kshitiz.taskforge.project.domain.port.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Database implementation of the {@link ProjectMemberRepository} port.
 */
@Component
@RequiredArgsConstructor
public class ProjectMemberPersistenceAdapter implements ProjectMemberRepository {

    private final ProjectMemberJpaRepository jpaRepository;
    private final ProjectPersistenceMapper mapper;

    /** {@inheritDoc} */
    @Override
    public ProjectMember save(ProjectMember member) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(member)));
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(UUID projectId, UUID userId) {
        return jpaRepository.existsById(new ProjectMemberJpaEntity.Key(projectId, userId));
    }

    /** {@inheritDoc} */
    @Override
    public void delete(UUID projectId, UUID userId) {
        jpaRepository.deleteById(new ProjectMemberJpaEntity.Key(projectId, userId));
    }

    /** {@inheritDoc} */
    @Override
    public List<ProjectMember> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public List<UUID> findProjectIdsByUserId(UUID userId) {
        return jpaRepository.findActiveProjectIdsByUserId(userId);
    }
}
