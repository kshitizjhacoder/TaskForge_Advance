package com.kshitiz.taskforge.ai.application;

import com.kshitiz.taskforge.ai.domain.port.AiUsageRepository;
import com.kshitiz.taskforge.common.exception.AiQuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiQuotaService} with a fixed clock.
 */
@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 12, 1);

    @Mock
    private AiUsageRepository usageRepository;

    private AiQuotaService quotaService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-01T23:59:00Z"), ZoneOffset.UTC);
        quotaService = new AiQuotaService(usageRepository, new AiProperties(3, 100, Duration.ofSeconds(5), 8), clock);
    }

    @Test
    void reservesWhenUnderBothLimits() {
        when(usageRepository.countForDay(TODAY)).thenReturn(10L);
        when(usageRepository.tryReserve(userId, TODAY, 3)).thenReturn(true);

        assertThatCode(() -> quotaService.reserve(userId)).doesNotThrowAnyException();
    }

    @Test
    void refusesWhenUserLimitReached() {
        when(usageRepository.countForDay(TODAY)).thenReturn(10L);
        when(usageRepository.tryReserve(userId, TODAY, 3)).thenReturn(false);

        assertThatThrownBy(() -> quotaService.reserve(userId))
                .isInstanceOf(AiQuotaExceededException.class)
                .hasMessageContaining("all 3 AI requests");
    }

    @Test
    void refusesWhenGlobalLimitReachedWithoutCountingTheUser() {
        when(usageRepository.countForDay(TODAY)).thenReturn(100L);

        assertThatThrownBy(() -> quotaService.reserve(userId)).isInstanceOf(AiQuotaExceededException.class);
        verify(usageRepository, never()).tryReserve(any(), any(), anyInt());
    }

    @Test
    void remainingNeverNegative() {
        when(usageRepository.countForUser(userId, TODAY)).thenReturn(5);

        assertThat(quotaService.remainingToday(userId)).isZero();
    }
}
