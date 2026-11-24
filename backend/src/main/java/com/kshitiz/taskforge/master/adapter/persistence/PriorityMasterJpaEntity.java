package com.kshitiz.taskforge.master.adapter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Row of the {@code master_priority} table. All columns come from {@link MasterDataJpaEntity}.
 */
@Entity
@Table(name = "master_priority")
public class PriorityMasterJpaEntity extends MasterDataJpaEntity {
}
