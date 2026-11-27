package com.kshitiz.taskforge.ai.adapter.persistence;

import com.kshitiz.taskforge.ai.domain.port.AiUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Database implementation of the {@link AiUsageRepository} port on the {@code ai_usage} table.
 */
@Component
@RequiredArgsConstructor
public class AiUsagePersistenceAdapter implements AiUsageRepository {

    private final AiUsageJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Two statements in one short transaction: make sure the day's row exists, then a
     * conditional increment that only succeeds while the count is below the limit.
     */
    @Override
    @Transactional
    public boolean tryReserve(UUID userId, LocalDate day, int limit) {
        jpaRepository.createRowIfMissing(toBytes(userId), day);
        return jpaRepository.incrementIfBelow(userId, day, limit) == 1;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public int countForUser(UUID userId, LocalDate day) {
        return jpaRepository.findById(new AiUsageJpaEntity.Key(userId, day))
                .map(AiUsageJpaEntity::getCallCount)
                .orElse(0);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public long countForDay(LocalDate day) {
        return jpaRepository.totalForDay(day);
    }

    /**
     * Converts a UUID to the 16-byte form stored in BINARY(16) columns (most significant
     * bits first, the same layout Hibernate uses for UUID fields).
     *
     * @param uuid the UUID
     * @return 16 bytes
     */
    static byte[] toBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }
}
