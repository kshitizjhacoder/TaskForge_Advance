package com.kshitiz.taskforge.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the application's clock. Code that needs "now" or "today" asks for this bean
 * instead of calling {@code Instant.now()}, so tests can replace it with a fixed clock.
 */
@Configuration
public class TimeConfig {

    /**
     * The real clock, in UTC so "today" means the same day on every server.
     *
     * @return UTC system clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
