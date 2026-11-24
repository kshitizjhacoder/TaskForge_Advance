package com.kshitiz.taskforge.master.adapter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Row of the {@code master_task_status} table. All columns come from {@link MasterDataJpaEntity}.
 */
@Entity
@Table(name = "master_task_status")
public class TaskStatusMasterJpaEntity extends MasterDataJpaEntity {
}
