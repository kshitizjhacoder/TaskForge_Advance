package com.kshitiz.taskforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the TaskForge backend.
 *
 * <p>Code is organised by feature ({@code master}, {@code user}, {@code task}, ...). Each feature
 * has three layers:
 * <ul>
 *   <li>{@code domain} – business objects and rules, plain Java, no framework code;</li>
 *   <li>{@code application} – services that run a use case and control transactions;</li>
 *   <li>{@code adapter} – the outside world: REST controllers and database access.</li>
 * </ul>
 * Code reused by several features lives in {@code common}.
 */
@SpringBootApplication
public class TaskForgeApplication {

    /**
     * Starts the Spring application context and the embedded web server.
     *
     * @param args command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(TaskForgeApplication.class, args);
    }
}
