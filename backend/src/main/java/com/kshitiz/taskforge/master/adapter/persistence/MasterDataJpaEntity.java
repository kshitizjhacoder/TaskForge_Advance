package com.kshitiz.taskforge.master.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Columns shared by every master table ({@code master_priority}, {@code master_task_status},
 * {@code master_role}). Each table has its own small subclass that only names the table.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class MasterDataJpaEntity {

    @Id
    @Column(name = "code", length = 30, nullable = false)
    private String code;

    @Column(name = "label", length = 60, nullable = false)
    private String label;

    @Column(name = "description")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "active", nullable = false)
    private boolean active;
}
