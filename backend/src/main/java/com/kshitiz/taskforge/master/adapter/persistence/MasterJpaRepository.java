package com.kshitiz.taskforge.master.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Query methods shared by every master-table repository. {@code @NoRepositoryBean} stops
 * Spring from trying to create a repository for this generic parent itself.
 *
 * @param <T> master entity type
 */
@NoRepositoryBean
public interface MasterJpaRepository<T extends MasterDataJpaEntity> extends JpaRepository<T, String> {

    /**
     * Loads every row sorted for display.
     *
     * @return all rows, lowest display order first
     */
    List<T> findAllByOrderByDisplayOrderAsc();
}
