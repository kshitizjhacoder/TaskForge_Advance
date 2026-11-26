package com.kshitiz.taskforge.common.adapter.storage;

import com.kshitiz.taskforge.common.exception.StorageUnavailableException;
import com.kshitiz.taskforge.common.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.io.InputStream;

/**
 * {@link FileStorage} implementation for any S3-compatible service (MinIO, Cloudflare R2,
 * AWS S3) using the AWS SDK v2.
 *
 * <p>All SDK errors are turned into {@link StorageUnavailableException} (HTTP 503), so
 * callers never see AWS-specific exceptions. Also implements {@link ApplicationRunner} to
 * create the bucket at startup when {@code create-bucket-if-missing} is enabled (local dev).
 */
@Slf4j
@Component
public class S3FileStorage implements FileStorage, ApplicationRunner {

    private static final String UNAVAILABLE = "File storage is temporarily unavailable. Please try again later.";

    private final S3Client s3;
    private final StorageProperties.S3 settings;

    /**
     * Creates the storage adapter.
     *
     * @param s3         configured S3 client
     * @param properties storage settings (bucket name, bucket creation flag)
     */
    public S3FileStorage(S3Client s3, StorageProperties properties) {
        this.s3 = s3;
        this.settings = properties.s3();
    }

    /** {@inheritDoc} */
    @Override
    public void store(String key, InputStream content, long sizeBytes, String contentType) {
        try {
            s3.putObject(request -> request.bucket(settings.bucket()).key(key).contentType(contentType),
                    RequestBody.fromInputStream(content, sizeBytes));
        } catch (SdkException ex) {
            throw new StorageUnavailableException(UNAVAILABLE, ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public InputStream open(String key) {
        try {
            return s3.getObject(request -> request.bucket(settings.bucket()).key(key));
        } catch (SdkException ex) {
            throw new StorageUnavailableException(UNAVAILABLE, ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(request -> request.bucket(settings.bucket()).key(key));
        } catch (SdkException ex) {
            throw new StorageUnavailableException(UNAVAILABLE, ex);
        }
    }

    /**
     * Creates the bucket at startup if enabled and missing. If storage is unreachable the
     * application still starts; uploads will then fail with 503 until it is back.
     *
     * @param args command-line arguments (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!settings.createBucketIfMissing()) {
            return;
        }
        try {
            s3.headBucket(request -> request.bucket(settings.bucket()));
        } catch (NoSuchBucketException missing) {
            s3.createBucket(request -> request.bucket(settings.bucket()));
            log.info("Created storage bucket '{}'", settings.bucket());
        } catch (SdkException ex) {
            log.warn("Could not reach file storage at startup ({}): {}", settings, ex.getMessage());
        }
    }
}
