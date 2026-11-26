package com.kshitiz.taskforge.task.application.dto;

import java.io.IOException;
import java.io.InputStream;

/**
 * A file received from the client, described without any web-framework types so the service
 * layer does not depend on Spring MVC's {@code MultipartFile}. The controller builds it.
 *
 * @param fileName    original file name from the client
 * @param contentType MIME type from the client
 * @param sizeBytes   size in bytes
 * @param content     opens the file's bytes (called once)
 */
public record UploadedFile(String fileName, String contentType, long sizeBytes, ContentSource content) {

    /**
     * Opens the uploaded bytes on demand.
     */
    @FunctionalInterface
    public interface ContentSource {

        /**
         * Opens a new stream over the file. The caller closes it.
         *
         * @return the file's bytes
         * @throws IOException if the upload cannot be read
         */
        InputStream open() throws IOException;
    }
}
