package com.kshitiz.taskforge.master.adapter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Row of the {@code master_role} table. All columns come from {@link MasterDataJpaEntity}.
 */
@Entity
@Table(name = "master_role")
public class RoleMasterJpaEntity extends MasterDataJpaEntity {
}
