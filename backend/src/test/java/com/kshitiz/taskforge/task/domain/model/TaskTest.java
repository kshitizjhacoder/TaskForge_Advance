package com.kshitiz.taskforge.task.domain.model;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the business rules in {@link Task} and {@link TaskStatus}. Plain Java, no
 * Spring, so they run in milliseconds.
 */
class TaskTest {

    private static final UUID CREATOR = UUID.randomUUID();
    private static final UUID PROJECT = UUID.randomUUID();
    private static final LocalDate DEC_1 = LocalDate.of(2026, 12, 1);
    private static final LocalDate DEC_10 = LocalDate.of(2026, 12, 10);

    /** Creates a valid task with the given tags; other values are fixed. */
    private static Task newTask(List<String> tags) {
        return Task.create(PROJECT, "  Write report  ", "  details ", CREATOR, DEC_1, DEC_10, null, tags);
    }

    @Nested
    class Create {

        @Test
        void startsInToDoWithMediumPriorityAndTrimmedText() {
            Task task = newTask(null);

            assertThat(task.getId()).isNotNull();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.TO_DO);
            assertThat(task.getPriority()).isEqualTo(Priority.MEDIUM);
            assertThat(task.getTitle()).isEqualTo("Write report");
            assertThat(task.getDescription()).isEqualTo("details");
            assertThat(task.getTags()).isEmpty();
        }

        @Test
        void requiresAProject() {
            assertThatThrownBy(() -> Task.create(null, "T", null, CREATOR, null, null, null, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("project");
        }

        @Test
        void rejectsBlankTitle() {
            assertThatThrownBy(() -> Task.create(PROJECT, "  ", null, CREATOR, null, null, null, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Title");
        }

        @Test
        void rejectsEndDateBeforeStartDate() {
            assertThatThrownBy(() -> Task.create(PROJECT, "T", null, CREATOR, DEC_10, DEC_1, null, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("End date");
        }

        @Test
        void cleansTags() {
            Task task = newTask(Arrays.asList(" Backend", "backend", "", null, "URGENT"));

            assertThat(task.getTags()).containsExactly("backend", "urgent");
        }

        @Test
        void rejectsTooManyTags() {
            List<String> tags = IntStream.rangeClosed(1, Task.MAX_TAGS + 1).mapToObj(i -> "tag" + i).toList();

            assertThatThrownBy(() -> newTask(tags)).isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    class Subtasks {

        @Test
        void subtaskInheritsProjectAndPointsToParent() {
            Task parent = newTask(null);

            Task subtask = Task.createSubtask(parent, "Step 1", null, CREATOR, null, null, null, null);

            assertThat(subtask.getProjectId()).isEqualTo(PROJECT);
            assertThat(subtask.getParentTaskId()).isEqualTo(parent.getId());
            assertThat(subtask.isSubtask()).isTrue();
            assertThat(subtask.getStatus()).isEqualTo(TaskStatus.TO_DO);
            assertThat(parent.isSubtask()).isFalse();
        }

        @Test
        void onlyOneLevelDeep() {
            Task subtask = Task.createSubtask(newTask(null), "Step 1", null, CREATOR, null, null, null, null);

            assertThatThrownBy(() -> Task.createSubtask(subtask, "Step 1.1", null, CREATOR, null, null, null, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Subtasks cannot have subtasks");
        }
    }

    @Nested
    class ChangeStatus {

        @ParameterizedTest(name = "{0} -> {1} is allowed")
        @CsvSource({
                "TO_DO, IN_PROGRESS", "TO_DO, BLOCKED",
                "IN_PROGRESS, DONE", "IN_PROGRESS, BLOCKED", "IN_PROGRESS, TO_DO",
                "BLOCKED, IN_PROGRESS", "BLOCKED, TO_DO",
                "DONE, IN_PROGRESS"})
        void allowsWorkflowMoves(TaskStatus from, TaskStatus to) {
            Task task = taskIn(from);

            task.changeStatus(to);

            assertThat(task.getStatus()).isEqualTo(to);
        }

        @ParameterizedTest(name = "{0} -> {1} is rejected")
        @CsvSource({"TO_DO, DONE", "BLOCKED, DONE", "DONE, TO_DO", "DONE, BLOCKED"})
        void rejectsInvalidMoves(TaskStatus from, TaskStatus to) {
            Task task = taskIn(from);

            assertThatThrownBy(() -> task.changeStatus(to))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot move");
        }

        @Test
        void sameStatusIsANoOp() {
            Task task = newTask(null);

            task.changeStatus(TaskStatus.TO_DO);

            assertThat(task.getStatus()).isEqualTo(TaskStatus.TO_DO);
        }

        /** Builds a task already in {@code status}, as if loaded from the database. */
        private Task taskIn(TaskStatus status) {
            return Task.restore().id(UUID.randomUUID()).title("T").createdBy(CREATOR)
                    .priority(Priority.LOW).status(status).tags(List.of()).build();
        }
    }

    @Nested
    class UpdateDetails {

        @Test
        void replacesDetails() {
            Task task = newTask(List.of("a"));

            task.updateDetails("New title", null, null, DEC_10, Priority.HIGH, List.of("b"));

            assertThat(task.getTitle()).isEqualTo("New title");
            assertThat(task.getDescription()).isNull();
            assertThat(task.getStartDate()).isNull();
            assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(task.getTags()).containsExactly("b");
        }

        @Test
        void keepsRulesOnUpdate() {
            Task task = newTask(null);

            assertThatThrownBy(() -> task.updateDetails("T", null, DEC_10, DEC_1, Priority.LOW, null))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }
}
