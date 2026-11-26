package com.kshitiz.taskforge.task.application.dto;

import java.io.InputStream;

/**
 * An attachment's bytes plus what the controller needs for the response headers.
 *
 * @param fileName    name for the Content-Disposition header
 * @param contentType MIME type for the Content-Type header
 * @param sizeBytes   size for the Content-Length header
 * @param content     the bytes; the controller streams and closes it
 */
public record AttachmentDownload(String fileName, String contentType, long sizeBytes, InputStream content) {
}
