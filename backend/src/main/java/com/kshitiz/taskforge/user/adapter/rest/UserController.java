package com.kshitiz.taskforge.user.adapter.rest;

import com.kshitiz.taskforge.common.adapter.rest.PageParams;
import com.kshitiz.taskforge.common.adapter.rest.RestResponses;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.user.application.UserService;
import com.kshitiz.taskforge.user.application.dto.AssignManagerCommand;
import com.kshitiz.taskforge.user.application.dto.CreateUserCommand;
import com.kshitiz.taskforge.user.application.dto.UserResponse;
import com.kshitiz.taskforge.user.domain.model.Role;
import com.kshitiz.taskforge.user.domain.model.UserSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for users. Controllers only translate HTTP to service calls; all rules
 * live in {@link UserService} and the domain.
 *
 * <p>Phase 2 moves sign-up to {@code /api/auth/register} and protects these endpoints.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    /**
     * Creates a user.
     *
     * @param command validated request body
     * @return 201 Created with the new user and its URL in the Location header
     */
    @PostMapping
    @Operation(summary = "Create a user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserCommand command) {
        UserResponse created = userService.createUser(command);
        return RestResponses.created(created, "/api/users/{id}", created.id());
    }

    /**
     * Returns one user.
     *
     * @param id user ID
     * @return the user (404 if not found)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID")
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    /**
     * Searches users with optional filters and paging.
     *
     * @param name       part of the full name (optional)
     * @param role       exact role (optional)
     * @param pageParams page, size, sort (fullName, email, createdAt) and direction
     * @return one page of users
     */
    @GetMapping
    @Operation(summary = "Search users")
    public PageResult<UserResponse> searchUsers(@RequestParam(required = false) String name,
                                                @RequestParam(required = false) Role role,
                                                @ParameterObject PageParams pageParams) {
        return userService.searchUsers(new UserSearchCriteria(name, role), pageParams.toPageQuery("fullName"));
    }

    /**
     * Sets or removes a user's manager.
     *
     * @param id      the user whose manager changes
     * @param command new manager ID, or null to remove
     * @return the updated user
     */
    @PutMapping("/{id}/manager")
    @Operation(summary = "Set or remove a user's manager")
    public UserResponse assignManager(@PathVariable UUID id, @RequestBody AssignManagerCommand command) {
        return userService.assignManager(id, command);
    }

    /**
     * Lists the users reporting directly to a manager.
     *
     * @param id the manager's ID
     * @return direct reports
     */
    @GetMapping("/{id}/direct-reports")
    @Operation(summary = "List a manager's direct reports")
    public List<UserResponse> getDirectReports(@PathVariable UUID id) {
        return userService.getDirectReports(id);
    }

    /**
     * Soft-deletes a user.
     *
     * @param id user ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
