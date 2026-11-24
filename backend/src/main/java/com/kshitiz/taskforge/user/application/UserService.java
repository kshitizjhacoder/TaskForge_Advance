package com.kshitiz.taskforge.user.application;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.exception.DuplicateResourceException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.user.application.dto.AssignManagerCommand;
import com.kshitiz.taskforge.user.application.dto.ChangeRoleCommand;
import com.kshitiz.taskforge.user.application.dto.CreateUserCommand;
import com.kshitiz.taskforge.user.application.dto.UserResponse;
import com.kshitiz.taskforge.user.domain.model.Role;
import com.kshitiz.taskforge.user.domain.model.User;
import com.kshitiz.taskforge.user.domain.model.UserSearchCriteria;
import com.kshitiz.taskforge.user.domain.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Use cases for managing users: create, look up, search, set a manager, delete.
 *
 * <p>Every public method runs in a database transaction; read-only methods use a read-only
 * transaction, which lets the database skip some locking work.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a user after checking the email is free. The password is hashed with BCrypt
     * before it is stored.
     *
     * @param command validated request data
     * @return the created user
     * @throws DuplicateResourceException if the email is already registered
     */
    @Transactional
    public UserResponse createUser(CreateUserCommand command) {
        String email = User.normalizeEmail(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with this email already exists");
        }
        User user = User.register(command.fullName(), email,
                passwordEncoder.encode(command.password()), command.role());
        User saved = userRepository.save(user);
        log.info("Created user {} with role {}", saved.getId(), saved.getRole());
        return UserResponse.from(saved);
    }

    /**
     * Returns one user.
     *
     * @param id user ID
     * @return the user
     * @throws ResourceNotFoundException if no such user exists
     */
    public UserResponse getUser(UUID id) {
        return UserResponse.from(loadUser(id));
    }

    /**
     * Returns one page of users matching optional filters.
     *
     * @param criteria  name and/or role filters
     * @param pageQuery page, size and sort
     * @return matching users
     */
    public PageResult<UserResponse> searchUsers(UserSearchCriteria criteria, PageQuery pageQuery) {
        return userRepository.search(criteria, pageQuery).map(UserResponse::from);
    }

    /**
     * Sets or removes a user's manager.
     *
     * @param userId  the user whose manager changes
     * @param command new manager ID, or {@code null} to remove the manager
     * @return the updated user
     * @throws ResourceNotFoundException      if the user or the manager does not exist
     * @throws BusinessRuleViolationException if the change would create a reporting loop
     */
    @Transactional
    public UserResponse assignManager(UUID userId, AssignManagerCommand command) {
        User user = loadUser(userId);
        if (command.managerId() == null) {
            user.removeManager();
        } else {
            User manager = loadUser(command.managerId());
            ensureNoReportingLoop(user, manager);
            user.assignManager(manager);
        }
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Changes a user's application role (admins only; enforced in the controller).
     *
     * @param userId  the user whose role changes
     * @param command the new role
     * @return the updated user
     * @throws ResourceNotFoundException      if the user does not exist
     * @throws BusinessRuleViolationException if this would remove the last admin
     */
    @Transactional
    public UserResponse changeRole(UUID userId, ChangeRoleCommand command) {
        User user = loadUser(userId);
        if (user.getRole() == Role.ADMIN && command.role() != Role.ADMIN) {
            ensureAnotherAdminRemains();
        }
        user.changeRole(command.role());
        log.info("User {} role changed to {}", userId, command.role());
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Lists the users who report directly to a manager.
     *
     * @param managerId the manager's ID
     * @return direct reports
     * @throws ResourceNotFoundException if the manager does not exist
     */
    public List<UserResponse> getDirectReports(UUID managerId) {
        loadUser(managerId);
        return userRepository.findByManagerId(managerId).stream()
                .map(UserResponse::from)
                .toList();
    }

    /**
     * Soft-deletes a user. Anyone who reported to them is left without a manager.
     *
     * @param id user ID
     * @throws ResourceNotFoundException      if no such user exists
     * @throws BusinessRuleViolationException if this is the last admin
     */
    @Transactional
    public void deleteUser(UUID id) {
        User user = loadUser(id);
        if (user.getRole() == Role.ADMIN) {
            ensureAnotherAdminRemains();
        }
        userRepository.clearManager(id);
        userRepository.deleteById(id);
        log.info("Deleted user {}", id);
    }

    /**
     * Checks that a user exists. Other features (tasks, comments) call this before linking
     * a user ID, so they get a clear 404 instead of a database error.
     *
     * @param id user ID
     * @throws ResourceNotFoundException if no such user exists
     */
    public void ensureUserExists(UUID id) {
        loadUser(id);
    }

    /**
     * Loads a user or throws 404.
     *
     * @param id user ID
     * @return the user
     */
    private User loadUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    /**
     * Stops an action that would leave the system without any admin (nobody could then
     * manage users or roles).
     *
     * @throws BusinessRuleViolationException if there is only one admin
     */
    private void ensureAnotherAdminRemains() {
        if (userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessRuleViolationException("The last admin cannot be removed or demoted");
        }
    }

    /**
     * Walks up the new manager's reporting chain. If it reaches {@code user}, assigning the
     * manager would create a loop (A manages B, B manages A), which is not allowed.
     *
     * @param user       the user getting a manager
     * @param newManager the proposed manager
     * @throws BusinessRuleViolationException if a loop would be created
     */
    private void ensureNoReportingLoop(User user, User newManager) {
        Set<UUID> visited = new HashSet<>();
        UUID current = newManager.getManagerId();
        while (current != null && visited.add(current)) {
            if (current.equals(user.getId())) {
                throw new BusinessRuleViolationException(
                        "This manager reports to the user (directly or indirectly), which would create a loop");
            }
            current = userRepository.findById(current).map(User::getManagerId).orElse(null);
        }
    }
}
