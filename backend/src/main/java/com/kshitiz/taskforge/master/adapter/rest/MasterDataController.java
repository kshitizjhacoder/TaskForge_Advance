package com.kshitiz.taskforge.master.adapter.rest;

import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.master.application.MasterDataService;
import com.kshitiz.taskforge.master.application.dto.MasterItemResponse;
import com.kshitiz.taskforge.master.domain.model.MasterType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only API for master data, used by the frontend to fill dropdowns, badges and filters
 * instead of hard-coding values.
 */
@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
@Tag(name = "Master data", description = "Lookup values: priorities, task statuses, roles")
public class MasterDataController {

    private final MasterDataService masterDataService;

    /**
     * Returns every master type in one response.
     *
     * @return map such as {"priorities": [...], "task-statuses": [...], "roles": [...]}
     */
    @GetMapping
    @Operation(summary = "Get all master data in one call")
    public Map<String, List<MasterItemResponse>> getAll() {
        Map<String, List<MasterItemResponse>> response = new LinkedHashMap<>();
        masterDataService.getAllActiveItems().forEach((slug, items) ->
                response.put(slug, items.stream().map(MasterItemResponse::from).toList()));
        return response;
    }

    /**
     * Returns the values of one master type.
     *
     * @param type URL name: "priorities", "task-statuses" or "roles"
     * @return active values in display order
     * @throws ResourceNotFoundException if the type name is unknown (404)
     */
    @GetMapping("/{type}")
    @Operation(summary = "Get one kind of master data")
    public List<MasterItemResponse> getByType(@PathVariable String type) {
        MasterType masterType = MasterType.fromSlug(type)
                .orElseThrow(() -> ResourceNotFoundException.of("Master data type", type));
        return masterDataService.getActiveItems(masterType).stream()
                .map(MasterItemResponse::from)
                .toList();
    }
}
