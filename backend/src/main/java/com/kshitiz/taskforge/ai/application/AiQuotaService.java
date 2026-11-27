package com.kshitiz.taskforge.ai.application;

import com.kshitiz.taskforge.ai.domain.port.AiUsageRepository;
import com.kshitiz.taskforge.common.exception.AiQuotaExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Enforces the daily AI limits, so one user cannot use up the shared free-tier quota and the
 * whole app stays under Gemini's free daily allowance.
 *
 * <p>A request is counted <em>before</em> the model is called, and stays counted even if the
 * call fails: the provider counts failed requests against our quota too.
 */
@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private final AiUsageRepository usageRepository;
    private final AiProperties properties;
    private final Clock clock;

    /**
     * Counts one AI request for the user, or refuses it.
     *
     * @param userId the user making the request
     * @throws AiQuotaExceededException if the app-wide or the user's daily limit is reached
     */
    public void reserve(UUID userId) {
        LocalDate today = today();
        if (usageRepository.countForDay(today) >= properties.globalDailyLimit()) {
            throw new AiQuotaExceededException(
                    "TaskForge has reached today's AI limit for all users. Please try again tomorrow.");
        }
        if (!usageRepository.tryReserve(userId, today, properties.userDailyLimit())) {
            throw new AiQuotaExceededException("You have used all " + properties.userDailyLimit()
                    + " AI requests for today. The limit resets at midnight UTC.");
        }
    }

    /**
     * Returns how many AI requests the user has left today.
     *
     * @param userId the user
     * @return remaining requests, never negative
     */
    public int remainingToday(UUID userId) {
        int used = usageRepository.countForUser(userId, today());
        return Math.max(0, properties.userDailyLimit() - used);
    }

    /**
     * Today's date in UTC.
     *
     * @return current UTC date
     */
    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
