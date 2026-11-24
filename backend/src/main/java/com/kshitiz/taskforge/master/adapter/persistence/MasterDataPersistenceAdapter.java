package com.kshitiz.taskforge.master.adapter.persistence;

import com.kshitiz.taskforge.master.domain.model.MasterItem;
import com.kshitiz.taskforge.master.domain.model.MasterType;
import com.kshitiz.taskforge.master.domain.port.MasterDataRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Database implementation of {@link MasterDataRepository}.
 *
 * <p>Holds one Spring Data repository per {@link MasterType}, so reading any master table is
 * a single map lookup instead of a switch statement repeated in several places.
 */
@Component
public class MasterDataPersistenceAdapter implements MasterDataRepository {

    private final Map<MasterType, MasterJpaRepository<? extends MasterDataJpaEntity>> repositories;
    private final MasterDataPersistenceMapper mapper;

    /**
     * Registers the repository for every master type.
     *
     * @param priorities   repository for {@code master_priority}
     * @param taskStatuses repository for {@code master_task_status}
     * @param roles        repository for {@code master_role}
     * @param mapper       entity-to-domain mapper
     */
    public MasterDataPersistenceAdapter(PriorityMasterJpaRepository priorities,
                                        TaskStatusMasterJpaRepository taskStatuses,
                                        RoleMasterJpaRepository roles,
                                        MasterDataPersistenceMapper mapper) {
        this.repositories = new EnumMap<>(MasterType.class);
        this.repositories.put(MasterType.PRIORITY, priorities);
        this.repositories.put(MasterType.TASK_STATUS, taskStatuses);
        this.repositories.put(MasterType.ROLE, roles);
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MasterItem> findAll(MasterType type) {
        MasterJpaRepository<? extends MasterDataJpaEntity> repository = repositories.get(type);
        if (repository == null) {
            throw new IllegalStateException("No repository registered for master type " + type);
        }
        return repository.findAllByOrderByDisplayOrderAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
