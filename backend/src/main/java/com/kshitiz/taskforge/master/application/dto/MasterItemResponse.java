package com.kshitiz.taskforge.master.application.dto;

import com.kshitiz.taskforge.master.domain.model.MasterItem;

/**
 * JSON shape of one master-data value returned by {@code /api/master}.
 *
 * @param code         value to send back to the API, e.g. "HIGH"
 * @param label        text to show, e.g. "High"
 * @param description  longer explanation, may be {@code null}
 * @param displayOrder sort position
 * @param color        hex colour for badges, may be {@code null}
 */
public record MasterItemResponse(String code, String label, String description,
                                 int displayOrder, String color) {

    /**
     * Converts a domain master item into its API representation.
     *
     * @param item domain object
     * @return response DTO
     */
    public static MasterItemResponse from(MasterItem item) {
        return new MasterItemResponse(item.code(), item.label(), item.description(),
                item.displayOrder(), item.color());
    }
}
