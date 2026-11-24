package com.kshitiz.taskforge.master.application;

import com.kshitiz.taskforge.master.domain.model.MasterItem;
import com.kshitiz.taskforge.master.domain.model.MasterType;
import com.kshitiz.taskforge.master.domain.port.MasterDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads master data (priorities, task statuses, roles) for the API and checks that the
 * database and the Java enums agree.
 *
 * <p>Results are cached because master data is read on almost every screen and changes only
 * through a migration (which comes with a restart).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterDataService {

    private final MasterDataRepository masterDataRepository;

    /**
     * Returns the active values of one master type, in display order.
     *
     * @param type which master data to read
     * @return active items, e.g. LOW, MEDIUM, HIGH, CRITICAL for priorities
     */
    @Cacheable(cacheNames = "master-data", key = "#type")
    public List<MasterItem> getActiveItems(MasterType type) {
        return loadActive(type);
    }

    /**
     * Returns the active values of every master type in one call, so the frontend can load
     * all dropdown options at start-up with a single request.
     *
     * @return map of URL name (e.g. "priorities") to its active items
     */
    @Cacheable(cacheNames = "master-data", key = "'all'")
    public Map<String, List<MasterItem>> getAllActiveItems() {
        Map<String, List<MasterItem>> result = new LinkedHashMap<>();
        for (MasterType type : MasterType.values()) {
            result.put(type.slug(), loadActive(type));
        }
        return result;
    }

    /**
     * Checks that each master table contains exactly the codes of its Java enum.
     *
     * <p>If a code exists in Java but not in the database, saving it would break the foreign
     * key. If a code exists in the database but not in Java, loading a row that uses it would
     * fail. Either way it is better to stop at start-up with a clear message.
     *
     * @throws IllegalStateException listing every mismatch found
     */
    public void verifyConsistency() {
        List<String> problems = new ArrayList<>();
        for (MasterType type : MasterType.values()) {
            Set<String> expected = type.expectedCodes();
            Set<String> actual = new HashSet<>();
            masterDataRepository.findAll(type).forEach(item -> actual.add(item.code()));

            Set<String> missingInDatabase = new HashSet<>(expected);
            missingInDatabase.removeAll(actual);
            Set<String> unknownToCode = new HashSet<>(actual);
            unknownToCode.removeAll(expected);

            if (!missingInDatabase.isEmpty()) {
                problems.add(type + ": missing in database " + missingInDatabase);
            }
            if (!unknownToCode.isEmpty()) {
                problems.add(type + ": not defined in Java enum " + unknownToCode);
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Master data does not match the Java enums: " + problems);
        }
        log.info("Master data verified for {} types", MasterType.values().length);
    }

    /**
     * Reads one master table and keeps only active rows.
     *
     * @param type which master data to read
     * @return active items in display order
     */
    private List<MasterItem> loadActive(MasterType type) {
        return masterDataRepository.findAll(type).stream()
                .filter(MasterItem::active)
                .toList();
    }
}
