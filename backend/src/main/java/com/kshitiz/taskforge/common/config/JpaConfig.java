package com.kshitiz.taskforge.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA auditing, which fills {@code created_at} and {@code updated_at}
 * on every entity that extends {@code BaseJpaEntity}.
 *
 * <p>Kept in its own class (not on the main application class) so web-layer slice tests
 * can start without a database.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
