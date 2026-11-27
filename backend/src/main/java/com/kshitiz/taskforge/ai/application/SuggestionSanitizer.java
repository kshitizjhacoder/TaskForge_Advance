package com.kshitiz.taskforge.ai.application;

import com.kshitiz.taskforge.ai.domain.model.SubtaskSuggestion;
import com.kshitiz.taskforge.task.domain.model.Priority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Output guardrail: cleans what the AI returned before any user sees it. AI output is
 * treated like any other untrusted input.
 *
 * <ul>
 *   <li>control characters are removed and text is trimmed;</li>
 *   <li>empty titles are dropped; titles and descriptions are cut to the database limits;</li>
 *   <li>duplicates (same title, any case) and copies of the parent task's title are dropped;</li>
 *   <li>a missing priority becomes MEDIUM;</li>
 *   <li>at most {@code maxSuggestions} are kept.</li>
 * </ul>
 */
public final class SuggestionSanitizer {

    static final int MAX_TITLE_LENGTH = 200;
    static final int MAX_DESCRIPTION_LENGTH = 2000;

    private SuggestionSanitizer() {
        // static helper, not meant to be instantiated
    }

    /**
     * Cleans a list of AI suggestions.
     *
     * @param raw            suggestions as returned by the AI (may be {@code null})
     * @param parentTitle    title of the task being split, to drop suggestions that just repeat it
     * @param maxSuggestions most suggestions to keep
     * @return clean suggestions, possibly empty, never {@code null}
     */
    public static List<SubtaskSuggestion> sanitize(List<SubtaskSuggestion> raw, String parentTitle,
                                                   int maxSuggestions) {
        if (raw == null) {
            return List.of();
        }
        Set<String> seenTitles = new HashSet<>();
        if (parentTitle != null) {
            seenTitles.add(parentTitle.trim().toLowerCase(Locale.ROOT));
        }
        List<SubtaskSuggestion> clean = new ArrayList<>();
        for (SubtaskSuggestion suggestion : raw) {
            if (suggestion == null || clean.size() >= maxSuggestions) {
                continue;
            }
            String title = cut(cleanText(suggestion.title()), MAX_TITLE_LENGTH);
            if (title == null || !seenTitles.add(title.toLowerCase(Locale.ROOT))) {
                continue;
            }
            String description = cut(cleanText(suggestion.description()), MAX_DESCRIPTION_LENGTH);
            Priority priority = suggestion.priority() == null ? Priority.MEDIUM : suggestion.priority();
            clean.add(new SubtaskSuggestion(title, description, priority));
        }
        return List.copyOf(clean);
    }

    /**
     * Removes control characters (keeping normal spaces) and trims.
     *
     * @param text raw text
     * @return cleaned text, or {@code null} if nothing is left
     */
    private static String cleanText(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.replaceAll("[\\p{Cntrl}&&[^\\n]]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Shortens text to a maximum length.
     *
     * @param text      text, may be {@code null}
     * @param maxLength longest allowed length
     * @return the text, cut if needed
     */
    private static String cut(String text, int maxLength) {
        return text == null || text.length() <= maxLength ? text : text.substring(0, maxLength).trim();
    }
}
