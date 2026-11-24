package com.kshitiz.taskforge.auth.application;

import com.kshitiz.taskforge.auth.application.dto.AuthResponse;
import com.kshitiz.taskforge.auth.application.dto.LoginCommand;
import com.kshitiz.taskforge.auth.application.dto.RefreshTokenCommand;
import com.kshitiz.taskforge.auth.application.dto.RegisterCommand;
import com.kshitiz.taskforge.auth.domain.port.TokenProvider;
import com.kshitiz.taskforge.common.exception.AuthenticationFailedException;
import com.kshitiz.taskforge.common.exception.DuplicateResourceException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.user.application.UserService;
import com.kshitiz.taskforge.user.application.dto.CreateUserCommand;
import com.kshitiz.taskforge.user.application.dto.UserResponse;
import com.kshitiz.taskforge.user.domain.model.Role;
import com.kshitiz.taskforge.user.domain.model.User;
import com.kshitiz.taskforge.user.domain.port.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for signing up, logging in and refreshing tokens.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String BAD_CREDENTIALS = "Invalid email or password";

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    /**
     * A real BCrypt hash of a random value. When the email is unknown, login still checks the
     * password against this hash, so the response takes as long as for a real user and an
     * attacker cannot tell which emails exist by timing the response.
     */
    private final String dummyPasswordHash;

    /**
     * Creates the service.
     *
     * @param userService     creates users (reuses email and password checks)
     * @param userRepository  loads users with their password hash
     * @param passwordEncoder checks passwords against stored hashes
     * @param tokenProvider   issues and checks tokens
     */
    public AuthService(UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenProvider tokenProvider) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.dummyPasswordHash = passwordEncoder.encode("timing-equaliser-" + System.nanoTime());
    }

    /**
     * Creates a MEMBER account and logs the new user in.
     *
     * @param command name, email and password
     * @return tokens and profile
     * @throws DuplicateResourceException if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterCommand command) {
        UserResponse created = userService.createUser(
                new CreateUserCommand(command.fullName(), command.email(), command.password(), Role.MEMBER));
        log.info("New member registered: {}", created.id());
        return issueFor(created);
    }

    /**
     * Checks email and password and returns new tokens.
     *
     * @param command email and password
     * @return tokens and profile
     * @throws AuthenticationFailedException if the email or password is wrong (same message for both)
     */
    public AuthResponse login(LoginCommand command) {
        User user = userRepository.findByEmail(User.normalizeEmail(command.email())).orElse(null);
        String hashToCheck = user != null ? user.getPasswordHash() : dummyPasswordHash;
        boolean passwordMatches = passwordEncoder.matches(command.password(), hashToCheck);
        if (user == null || !passwordMatches) {
            log.info("Failed login attempt");
            throw new AuthenticationFailedException(BAD_CREDENTIALS);
        }
        return issueFor(UserResponse.from(user));
    }

    /**
     * Exchanges a valid refresh token for a new token pair. The user is reloaded from the
     * database, so a deleted user cannot refresh and a changed role is picked up.
     *
     * @param command the refresh token
     * @return new tokens and profile
     * @throws AuthenticationFailedException if the token is invalid/expired or the user no longer exists
     */
    public AuthResponse refresh(RefreshTokenCommand command) {
        var userId = tokenProvider.parseRefreshToken(command.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid or expired token"));
        return issueFor(UserResponse.from(user));
    }

    /**
     * Issues tokens for a user and builds the response.
     *
     * @param user the user's profile
     * @return tokens and profile
     */
    private AuthResponse issueFor(UserResponse user) {
        CurrentUser principal = new CurrentUser(user.id(), user.email(), user.role());
        return AuthResponse.of(tokenProvider.issueTokens(principal), user);
    }
}
