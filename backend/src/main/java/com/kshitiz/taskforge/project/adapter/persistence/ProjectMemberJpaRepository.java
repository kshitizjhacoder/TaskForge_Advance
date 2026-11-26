package com.kshitiz.taskforge.project.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link ProjectMemberJpaEntity}.
 */
public interface ProjectMemberJpaRepository
        extends JpaRepository<ProjectMemberJpaEntity, ProjectMemberJpaEntity.Key> {

    /**
     * Lists a project's members.
     *
     * @param projectId project ID
     * @return memberships, earliest first
     */
    List<ProjectMemberJpaEntity> findByProjectIdOrderByJoinedAtAsc(UUID projectId);

    /**
     * Lists the IDs of the non-deleted projects a user belongs to. The join to
     * {@code ProjectJpaEntity} applies its soft-delete filter.
     *
     * @param userId user ID
     * @return project IDs
     */
    @Query("""
            select m.projectId from ProjectMemberJpaEntity m
            where m.userId = :userId
              and exists (select p.id from ProjectJpaEntity p where p.id = m.projectId)
            """)
    List<UUID> findActiveProjectIdsByUserId(@Param("userId") UUID userId);
}
