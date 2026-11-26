package com.kshitiz.taskforge.project.adapter.rest;

import com.kshitiz.taskforge.common.adapter.rest.PageParams;
import com.kshitiz.taskforge.common.adapter.rest.RestResponses;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.application.ProjectService;
import com.kshitiz.taskforge.project.application.dto.AddMemberCommand;
import com.kshitiz.taskforge.project.application.dto.CreateProjectCommand;
import com.kshitiz.taskforge.project.application.dto.ProjectMemberResponse;
import com.kshitiz.taskforge.project.application.dto.ProjectResponse;
import com.kshitiz.taskforge.project.application.dto.UpdateProjectCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for projects and their members. Only managers and admins may create
 * projects; everything else is checked against membership and ownership in
 * {@link ProjectService}.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Creates a project owned by the logged-in user.
     *
     * @param command key, name and description
     * @param actor   the logged-in user (becomes owner)
     * @return 201 Created with the project (409 if the key is taken)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create a project (manager or admin)")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectCommand command,
                                                         @AuthenticationPrincipal CurrentUser actor) {
        ProjectResponse created = projectService.createProject(command, actor);
        return RestResponses.created(created, "/api/projects/{id}", created.id());
    }

    /**
     * Lists the projects the logged-in user can see.
     *
     * @param actor      the logged-in user
     * @param pageParams page, size, sort (name, key, createdAt) and direction
     * @return one page of projects
     */
    @GetMapping
    @Operation(summary = "List my projects (all projects for admins)")
    public PageResult<ProjectResponse> listProjects(@AuthenticationPrincipal CurrentUser actor,
                                                    @ParameterObject PageParams pageParams) {
        return projectService.listProjects(actor, pageParams.toPageQuery("name"));
    }

    /**
     * Returns one project.
     *
     * @param id    project ID
     * @param actor the logged-in user
     * @return the project (404 if not found or not a member)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a project")
    public ProjectResponse getProject(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser actor) {
        return projectService.getProject(id, actor);
    }

    /**
     * Changes a project's name and description.
     *
     * @param id      project ID
     * @param command new details
     * @param actor   the logged-in user
     * @return the updated project (403 if not owner or admin)
     */
    @PutMapping("/{id}")
    @Operation(summary = "Edit a project (owner or admin)")
    public ProjectResponse updateProject(@PathVariable UUID id, @Valid @RequestBody UpdateProjectCommand command,
                                         @AuthenticationPrincipal CurrentUser actor) {
        return projectService.updateProject(id, command, actor);
    }

    /**
     * Soft-deletes a project.
     *
     * @param id    project ID
     * @param actor the logged-in user
     * @return 204 No Content (403 if not owner or admin)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project (owner or admin)")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser actor) {
        projectService.deleteProject(id, actor);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists a project's members.
     *
     * @param id    project ID
     * @param actor the logged-in user
     * @return members
     */
    @GetMapping("/{id}/members")
    @Operation(summary = "List project members")
    public List<ProjectMemberResponse> listMembers(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser actor) {
        return projectService.listMembers(id, actor);
    }

    /**
     * Adds a user to a project.
     *
     * @param id      project ID
     * @param command the user to add
     * @param actor   the logged-in user
     * @return 201 Created with the membership (409 if already a member)
     */
    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member (owner or admin)")
    public ResponseEntity<ProjectMemberResponse> addMember(@PathVariable UUID id,
                                                           @Valid @RequestBody AddMemberCommand command,
                                                           @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addMember(id, command, actor));
    }

    /**
     * Removes a user from a project (and from its tasks' assignees).
     *
     * @param id     project ID
     * @param userId user to remove
     * @param actor  the logged-in user
     * @return 204 No Content (422 if it is the owner)
     */
    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove a member (owner or admin)")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID userId,
                                             @AuthenticationPrincipal CurrentUser actor) {
        projectService.removeMember(id, userId, actor);
        return ResponseEntity.noContent().build();
    }
}
