package com.kshitiz.taskforge.common.storage;

import com.kshitiz.taskforge.common.exception.StorageUnavailableException;

import java.io.InputStream;

/**
 * Port (interface) for storing file contents. The S3 implementation is
 * {@code S3FileStorage}; tests use an in-memory fake. Callers only deal with keys and
 * streams, so the storage provider (MinIO, Cloudflare R2, AWS S3, ...) can change without
 * touching any feature code.
 */
public interface FileStorage {

    /**
     * Stores a file under a key, replacing anything already stored there.
     *
     * @param key         unique location, e.g. "tasks/{taskId}/{attachmentId}"
     * @param content     the bytes to store (read to the end, not closed)
     * @param sizeBytes   exact number of bytes in {@code content}
     * @param contentType MIME type, e.g. "application/pdf"
     * @throws StorageUnavailableException if the storage service cannot be reached
     */
    void store(String key, InputStream content, long sizeBytes, String contentType);

    /**
     * Opens a stored file for reading. The caller must close the stream.
     *
     * @param key location used when storing
     * @return stream of the file's bytes
     * @throws StorageUnavailableException if the file cannot be read
     */
    InputStream open(String key);

    /**
     * Deletes a stored file. Deleting a key that does not exist is not an error.
     *
     * @param key location used when storing
     * @throws StorageUnavailableException if the storage service cannot be reached
     */
    void delete(String key);
}
