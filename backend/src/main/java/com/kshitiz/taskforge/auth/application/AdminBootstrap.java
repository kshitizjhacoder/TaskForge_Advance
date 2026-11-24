package com.kshitiz.taskforge.auth.application;

import com.kshitiz.taskforge.auth.config.SecurityProperties;
import com.kshitiz.taskforge.common.exception.DuplicateResourceException;
import com.kshitiz.taskforge.user.application.UserService;
import com.kshitiz.taskforge.user.application.dto.CreateUserCommand;
import com.kshitiz.taskforge.user.domain.model.Role;
import com.kshitiz.taskforge.user.domain.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin account at startup.
 *
 * <p>Public sign-up only creates MEMBERs, so without this nobody could ever become an admin.
 * It runs only when no admin exists yet and {@code ADMIN_EMAIL}/{@code ADMIN_PASSWORD} are
 * set, so restarting the application never creates duplicates or resets a password.
 *
 * <p>Runs after the master-data check ({@code @Order(2)}), because creating a user needs the
 * role table.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final SecurityProperties securityProperties;
    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Creates the configured admin if there is no admin yet.
     *
     * @param args command-line arguments (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        SecurityProperties.BootstrapAdmin admin = securityProperties.bootstrapAdmin();
        if (admin == null || !admin.isConfigured()) {
            return;
        }
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            return;
        }
        try {
            userService.createUser(new CreateUserCommand(admin.fullName(), admin.email(), admin.password(), Role.ADMIN));
            log.info("Created initial admin account {}", admin.email());
        } catch (DuplicateResourceException ex) {
            log.warn("Initial admin not created: a non-admin user already uses {}", admin.email());
        }
    }
}
