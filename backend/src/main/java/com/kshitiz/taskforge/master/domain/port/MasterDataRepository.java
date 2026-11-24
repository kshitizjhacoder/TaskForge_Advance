package com.kshitiz.taskforge.master.domain.port;

import com.kshitiz.taskforge.master.domain.model.MasterItem;
import com.kshitiz.taskforge.master.domain.model.MasterType;

import java.util.List;

/**
 * Port (interface) for reading master data. The database implementation is
 * {@code MasterDataPersistenceAdapter}.
 */
public interface MasterDataRepository {

    /**
     * Loads every row of one master table, active and inactive.
     *
     * @param type which master table to read
     * @return rows sorted by display order
     */
    List<MasterItem> findAll(MasterType type);
}
