package com.kshitiz.taskforge.ai.application;

import com.kshitiz.taskforge.ai.domain.model.SubtaskSuggestion;
import com.kshitiz.taskforge.task.domain.model.Priority;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the AI output guardrail {@link SuggestionSanitizer}.
 */
class SuggestionSanitizerTest {

    @Test
    void cleansTextAndDefaultsPriority() {
        List<SubtaskSuggestion> clean = SuggestionSanitizer.sanitize(
                List.of(new SubtaskSuggestion("  Write\ttests \u0007 ", "  Cover edge cases ", null)), "Parent", 8);

        assertThat(clean).containsExactly(new SubtaskSuggestion("Write tests", "Cover edge cases", Priority.MEDIUM));
    }

    @Test
    void dropsBlankDuplicateAndParentTitles() {
        List<SubtaskSuggestion> clean = SuggestionSanitizer.sanitize(Arrays.asList(
                new SubtaskSuggestion("Design schema", null, Priority.HIGH),
                new SubtaskSuggestion("design SCHEMA", null, Priority.LOW),
                new SubtaskSuggestion("   ", "no title", Priority.LOW),
                new SubtaskSuggestion("Fix login bug", null, Priority.LOW),
                null), "Fix login bug", 8);

        assertThat(clean).extracting(SubtaskSuggestion::title).containsExactly("Design schema");
    }

    @Test
    void cutsLongTextAndLimitsCount() {
        String longTitle = "x".repeat(500);
        List<SubtaskSuggestion> many = IntStream.range(0, 20)
                .mapToObj(i -> new SubtaskSuggestion(i == 0 ? longTitle : "Step " + i, "d".repeat(5000), null))
                .toList();

        List<SubtaskSuggestion> clean = SuggestionSanitizer.sanitize(many, "Parent", 5);

        assertThat(clean).hasSize(5);
        assertThat(clean.get(0).title()).hasSize(SuggestionSanitizer.MAX_TITLE_LENGTH);
        assertThat(clean.get(0).description()).hasSize(SuggestionSanitizer.MAX_DESCRIPTION_LENGTH);
    }

    @Test
    void nullInputGivesEmptyList() {
        assertThat(SuggestionSanitizer.sanitize(null, "Parent", 5)).isEmpty();
    }
}
