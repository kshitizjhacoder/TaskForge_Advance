package com.kshitiz.taskforge.master.domain.model;

/**
 * One row of master data, e.g. the priority HIGH with its label and colour.
 *
 * @param code         stable identifier stored in business tables, e.g. "HIGH"
 * @param label        text shown to users, e.g. "High"
 * @param description  longer explanation (may be {@code null})
 * @param displayOrder position in dropdowns and lists (ascending)
 * @param color        hex colour for badges, e.g. "#F59E0B" (may be {@code null})
 * @param active       {@code false} hides the value from new selections
 */
public record MasterItem(String code, String label, String description,
                         int displayOrder, String color, boolean active) {
}
