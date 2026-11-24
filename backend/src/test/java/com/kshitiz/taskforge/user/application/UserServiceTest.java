package com.kshitiz.taskforge.user.application;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.exception.DuplicateResourceException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.user.application.dto.AssignManagerCommand;
import com.kshitiz.taskforge.user.application.dto.CreateUserCommand;
import com.kshitiz.taskforge.user.application.dto.UserResponse;
import com.kshitiz.taskforge.user.domain.model.Role;
import com.kshitiz.taskforge.user.domain.model.User;
import com.kshitiz.taskforge.user.domain.port.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}. The repository and password encoder are mocks, so
 * only the service's own logic is tested.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    /** Builds a stored user with the given ID and manager. */
    private static User storedUser(UUID id, UUID managerId) {
        return User.restore().id(id).fullName("User " + id).email(id + "@test.dev")
                .passwordHash("hash").role(Role.MEMBER).managerId(managerId).build();
    }

    @Test
    void createUserHashesPasswordAndNormalisesEmail() {
        when(userRepository.existsByEmail("ravi@test.dev")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserResponse response = userService.createUser(
                new CreateUserCommand("Ravi", "  Ravi@Test.dev ", "secret123", Role.MEMBER));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(response.email()).isEqualTo("ravi@test.dev");
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ravi@test.dev")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserCommand("Ravi", "ravi@test.dev", "secret123", Role.MEMBER)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignManagerRejectsReportingLoop() {
        // A is managed by B. Making A the manager of B would create a loop.
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(userRepository.findById(b)).thenReturn(Optional.of(storedUser(b, null)));
        when(userRepository.findById(a)).thenReturn(Optional.of(storedUser(a, b)));

        assertThatThrownBy(() -> userService.assignManager(b, new AssignManagerCommand(a)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("loop");
    }

    @Test
    void assignManagerRejectsSelf() {
        UUID a = UUID.randomUUID();
        when(userRepository.findById(a)).thenReturn(Optional.of(storedUser(a, null)));

        assertThatThrownBy(() -> userService.assignManager(a, new AssignManagerCommand(a)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
