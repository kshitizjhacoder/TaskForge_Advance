package com.kshitiz.taskforge.project.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Spring Data repository for {@link ProjectJpaEntity}.
 */
public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, UUID> {

    /**
     * Checks whether a key is used by a non-deleted project.
     *
     * @param key upper-case project key
     * @return {@code true} if taken
     */
    boolean existsByKey(String key);

    /**
     * Lists the projects a user is a member of.
     *
     * @param userId   the member
     * @param pageable page, size and sort
     * @return one page of projects
     */
    @Query(value = """
            select p from ProjectJpaEntity p
            where exists (select m.projectId from ProjectMemberJpaEntity m
                          where m.projectId = p.id and m.userId = :userId)
            """)
    Page<ProjectJpaEntity> findByMember(@Param("userId") UUID userId, Pageable pageable);
}
