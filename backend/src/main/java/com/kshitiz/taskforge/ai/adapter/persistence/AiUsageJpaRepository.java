package com.kshitiz.taskforge.ai.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Spring Data repository for {@link AiUsageJpaEntity}.
 */
public interface AiUsageJpaRepository extends JpaRepository<AiUsageJpaEntity, AiUsageJpaEntity.Key> {

    /**
     * Creates the user's counter row for a day if it does not exist yet. {@code INSERT IGNORE}
     * makes this safe when two requests try to create the row at the same moment.
     *
     * @param userIdBytes the user's UUID as 16 bytes (the column type is BINARY(16))
     * @param day         the day (UTC)
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO ai_usage (user_id, usage_date, call_count) VALUES (:userId, :day, 0)",
            nativeQuery = true)
    void createRowIfMissing(@Param("userId") byte[] userIdBytes, @Param("day") LocalDate day);

    /**
     * Adds one to the user's counter, but only while it is below the limit. Check and
     * increment happen in a single statement, so concurrent requests cannot exceed the limit.
     *
     * @param userId user ID
     * @param day    the day (UTC)
     * @param limit  the daily limit
     * @return 1 if counted, 0 if the limit was already reached
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update AiUsageJpaEntity u set u.callCount = u.callCount + 1
            where u.userId = :userId and u.usageDate = :day and u.callCount < :limit
            """)
    int incrementIfBelow(@Param("userId") UUID userId, @Param("day") LocalDate day, @Param("limit") int limit);

    /**
     * Adds up all users' requests on a day.
     *
     * @param day the day (UTC)
     * @return total requests (0 if none)
     */
    @Query("select coalesce(sum(u.callCount), 0) from AiUsageJpaEntity u where u.usageDate = :day")
    long totalForDay(@Param("day") LocalDate day);
}
