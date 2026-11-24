package com.kshitiz.taskforge.master.application;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs the master-data check once, right after the application starts. If the check fails
 * the application stops, so a mismatch between code and database is found on deploy rather
 * than by a user. Runs first ({@code @Order(1)}), before anything that reads master data.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class MasterDataConsistencyCheck implements ApplicationRunner {

    private final MasterDataService masterDataService;

    /**
     * Called by Spring Boot after start-up; delegates to
     * {@link MasterDataService#verifyConsistency()}.
     *
     * @param args command-line arguments (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        masterDataService.verifyConsistency();
    }
}
