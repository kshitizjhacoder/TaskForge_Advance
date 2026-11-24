package com.kshitiz.taskforge.common.adapter.persistence;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Generic building blocks for search filters (JPA Specifications), shared by every
 * feature's search so the same WHERE-clause code is not rewritten per entity.
 *
 * <p>Every method returns {@code null} when the filter value is empty. Spring Data skips
 * {@code null} specifications, so callers can combine filters without if-statements.
 */
public final class Specs {

    private Specs() {
        // utility class, not meant to be instantiated
    }

    /**
     * {@code field = value}.
     *
     * @param field entity property name
     * @param value value to match, or {@code null} for no filter
     * @param <T>   entity type
     * @return the condition, or {@code null}
     */
    public static <T> Specification<T> equalTo(String field, Object value) {
        if (value == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    /**
     * {@code lower(field) LIKE %text%}: case-insensitive "contains". Characters that have a
     * special meaning in LIKE ({@code %}, {@code _}) are escaped so they match literally.
     *
     * @param field entity property name (a text column)
     * @param text  text to look for, or {@code null}/blank for no filter
     * @param <T>   entity type
     * @return the condition, or {@code null}
     */
    public static <T> Specification<T> containsIgnoreCase(String field, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(field)), pattern, '\\');
    }

    /**
     * {@code field >= value}.
     *
     * @param field entity property name
     * @param value lower bound (inclusive), or {@code null} for no filter
     * @param <T>   entity type
     * @param <V>   comparable value type, e.g. LocalDate
     * @return the condition, or {@code null}
     */
    public static <T, V extends Comparable<? super V>> Specification<T> atLeast(String field, V value) {
        if (value == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(field), value);
    }

    /**
     * {@code field <= value}.
     *
     * @param field entity property name
     * @param value upper bound (inclusive), or {@code null} for no filter
     * @param <T>   entity type
     * @param <V>   comparable value type, e.g. LocalDate
     * @return the condition, or {@code null}
     */
    public static <T, V extends Comparable<? super V>> Specification<T> atMost(String field, V value) {
        if (value == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(field), value);
    }

    /**
     * Escapes LIKE wildcards so user input is matched literally.
     *
     * @param text raw search text
     * @return text with {@code \}, {@code %} and {@code _} escaped
     */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
