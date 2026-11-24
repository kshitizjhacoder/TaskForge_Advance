package com.kshitiz.taskforge.auth.application;

import com.kshitiz.taskforge.auth.application.dto.AuthResponse;
import com.kshitiz.taskforge.auth.application.dto.LoginCommand;
import com.kshitiz.taskforge.auth.application.dto.RefreshTokenCommand;
import com.kshitiz.taskforge.auth.domain.model.IssuedTokens;
import com.kshitiz.taskforge.auth.domain.port.TokenProvider;
import com.kshitiz.taskforge.common.exception.AuthenticationFailedException;
import com.kshitiz.taskforge.user.application.UserService;
import com.kshitiz.taskforge.user.domain.model.Role;
import com.kshitiz.taskforge.user.domain.model.User;
import com.kshitiz.taskforge.user.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}: login success and failure, and refresh for a deleted user.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenProvider tokenProvider;

    private AuthService authService;
    private User asha;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        authService = new AuthService(userService, userRepository, passwordEncoder, tokenProvider);
        asha = User.restore().id(UUID.randomUUID()).fullName("Asha").email("asha@test.dev")
                .passwordHash("asha-hash").role(Role.MEMBER).build();
    }

    @Test
    void loginWithCorrectPasswordReturnsTokens() {
        when(userRepository.findByEmail("asha@test.dev")).thenReturn(Optional.of(asha));
        when(passwordEncoder.matches("secret123", "asha-hash")).thenReturn(true);
        when(tokenProvider.issueTokens(any())).thenReturn(new IssuedTokens("access", "refresh", Duration.ofMinutes(15)));

        AuthResponse response = authService.login(new LoginCommand(" Asha@Test.dev ", "secret123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.user().id()).isEqualTo(asha.getId());
    }

    @Test
    void wrongPasswordIsRejected() {
        when(userRepository.findByEmail("asha@test.dev")).thenReturn(Optional.of(asha));
        when(passwordEncoder.matches("wrong", "asha-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand("asha@test.dev", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void unknownEmailGetsSameErrorAndStillChecksAPassword() {
        when(userRepository.findByEmail("nobody@test.dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand("nobody@test.dev", "whatever")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
        // The dummy hash is checked so the response time does not reveal unknown emails
        verify(passwordEncoder).matches(eq("whatever"), eq("dummy-hash"));
    }

    @Test
    void refreshForDeletedUserIsRejected() {
        UUID deletedUserId = UUID.randomUUID();
        when(tokenProvider.parseRefreshToken("refresh")).thenReturn(deletedUserId);
        when(userRepository.findById(deletedUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand("refresh")))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
