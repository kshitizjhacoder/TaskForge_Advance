package com.kshitiz.taskforge.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's caching annotations ({@code @Cacheable}).
 *
 * <p>Used for master data, which is read on almost every screen but changes only through a
 * database migration. The cache is a simple in-memory map (configured in
 * {@code application.yml}); a restart clears it.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
