package com.kshitiz.taskforge.task.adapter.persistence;

import com.kshitiz.taskforge.common.adapter.persistence.Specs;
import com.kshitiz.taskforge.task.domain.model.TaskSearchCriteria;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Builds the WHERE clause for task search from {@link TaskSearchCriteria}. Unset filters are
 * skipped automatically (see {@link Specs}).
 */
final class TaskSpecifications {

    private TaskSpecifications() {
        // utility class, not meant to be instantiated
    }

    /**
     * Combines every filter that is set into one condition (all must match).
     *
     * @param criteria search filters
     * @return combined condition
     */
    static Specification<TaskJpaEntity> matching(TaskSearchCriteria criteria) {
        return Specification.allOf(
                Specs.containsIgnoreCase("title", criteria.text()),
                Specs.equalTo("status", criteria.status()),
                Specs.equalTo("priority", criteria.priority()),
                Specs.equalTo("projectId", criteria.projectId()),
                Specs.in("projectId", criteria.visibleProjectIds()),
                Specs.equalTo("createdBy", criteria.createdBy()),
                assignedTo(criteria.assigneeId()),
                Specs.atLeast("endDate", criteria.dueFrom()),
                Specs.atMost("endDate", criteria.dueTo()));
    }

    /**
     * Task has {@code userId} among its assignees:
     * {@code id IN (SELECT task_id FROM task_assignments WHERE user_id = ?)}.
     *
     * @param userId assignee to look for, or {@code null}
     * @return the condition, or {@code null} if no filter
     */
    private static Specification<TaskJpaEntity> assignedTo(UUID userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<UUID> assignedTaskIds = query.subquery(UUID.class);
            Root<TaskAssignmentJpaEntity> assignment = assignedTaskIds.from(TaskAssignmentJpaEntity.class);
            assignedTaskIds.select(assignment.get("taskId"))
                    .where(cb.equal(assignment.get("userId"), userId));
            return root.get("id").in(assignedTaskIds);
        };
    }
}
