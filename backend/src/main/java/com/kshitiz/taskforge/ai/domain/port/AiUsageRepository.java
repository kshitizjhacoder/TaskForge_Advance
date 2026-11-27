package com.kshitiz.taskforge.ai.domain.port;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Port (interface) for counting AI requests per user per day. Implemented by
 * {@code AiUsagePersistenceAdapter} on the {@code ai_usage} table.
 */
public interface AiUsageRepository {

    /**
     * Counts one more request for a user on a day, but only if they are still under the
     * limit. The check and the increment happen in one database statement, so two requests
     * arriving at the same moment cannot both slip past the limit.
     *
     * @param userId the user
     * @param day    the day (UTC)
     * @param limit  the user's daily limit
     * @return {@code true} if the request was counted, {@code false} if the limit was reached
     */
    boolean tryReserve(UUID userId, LocalDate day, int limit);

    /**
     * Returns how many requests a user made on a day.
     *
     * @param userId the user
     * @param day    the day (UTC)
     * @return number of requests (0 if none)
     */
    int countForUser(UUID userId, LocalDate day);

    /**
     * Returns how many requests all users together made on a day.
     *
     * @param day the day (UTC)
     * @return total number of requests
     */
    long countForDay(LocalDate day);
}
