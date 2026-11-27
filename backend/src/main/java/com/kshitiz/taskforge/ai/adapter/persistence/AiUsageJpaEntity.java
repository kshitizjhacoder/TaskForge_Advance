package com.kshitiz.taskforge.ai.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Database row of the {@code ai_usage} table: how many AI requests one user made on one day.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_usage")
@IdClass(AiUsageJpaEntity.Key.class)
public class AiUsageJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "call_count", nullable = false)
    private int callCount;

    /**
     * Composite primary key (user + day), required by {@code @IdClass}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID userId;
        private LocalDate usageDate;

        /**
         * Creates a key.
         *
         * @param userId    user ID
         * @param usageDate day (UTC)
         */
        public Key(UUID userId, LocalDate usageDate) {
            this.userId = userId;
            this.usageDate = usageDate;
        }
    }
}
