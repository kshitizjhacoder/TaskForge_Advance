package com.kshitiz.taskforge.task.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Spring Data repository for {@link TaskJpaEntity}. {@link JpaSpecificationExecutor} adds
 * the dynamic filtering used by task search.
 */
public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID>,
        JpaSpecificationExecutor<TaskJpaEntity> {
}
