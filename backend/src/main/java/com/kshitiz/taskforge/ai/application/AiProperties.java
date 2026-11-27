package com.kshitiz.taskforge.ai.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI limits, bound from {@code taskforge.ai.*} in {@code application.yml}. The provider and
 * model themselves are configured under {@code spring.ai.*}.
 *
 * @param userDailyLimit   AI requests one user may make per UTC day
 * @param globalDailyLimit AI requests all users together may make per UTC day; keep it below
 *                         the Gemini free-tier daily quota
 * @param timeout          how long to wait for the model before giving up
 * @param maxSuggestions   most subtasks the AI may propose for one task
 */
@ConfigurationProperties(prefix = "taskforge.ai")
public record AiProperties(int userDailyLimit, int globalDailyLimit, Duration timeout, int maxSuggestions) {
}
