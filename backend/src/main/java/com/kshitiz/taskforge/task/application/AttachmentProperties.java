package com.kshitiz.taskforge.task.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;
import java.util.Locale;

/**
 * Upload rules for task attachments, bound from {@code taskforge.attachments.*}.
 *
 * @param maxFileSize          largest accepted file, e.g. 10MB
 * @param allowedContentTypes  MIME types that may be uploaded (others are rejected)
 */
@ConfigurationProperties(prefix = "taskforge.attachments")
public record AttachmentProperties(DataSize maxFileSize, List<String> allowedContentTypes) {

    /**
     * Checks whether a MIME type may be uploaded. Parameters such as "; charset=utf-8" and
     * letter case are ignored.
     *
     * @param contentType MIME type sent by the client
     * @return {@code true} if allowed
     */
    public boolean isAllowed(String contentType) {
        return allowedContentTypes.contains(normalize(contentType));
    }

    /**
     * Reduces a MIME type to its lower-case base form: "Text/Plain; charset=UTF-8" becomes
     * "text/plain".
     *
     * @param contentType MIME type, may be {@code null}
     * @return normalised type, or an empty string if {@code null}
     */
    public static String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }
}
