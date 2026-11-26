package com.kshitiz.taskforge.common.adapter.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * File storage settings, bound from {@code taskforge.storage.*} in {@code application.yml}.
 *
 * @param s3 connection details for the S3-compatible storage service
 */
@ConfigurationProperties(prefix = "taskforge.storage")
public record StorageProperties(S3 s3) {

    /**
     * S3-compatible storage connection.
     *
     * @param endpoint              service URL (e.g. http://localhost:9000 for MinIO);
     *                              blank means the AWS default for the region
     * @param region                region name ("auto" for Cloudflare R2)
     * @param bucket                bucket that holds all TaskForge files
     * @param accessKey             access key ID
     * @param secretKey             secret access key
     * @param pathStyleAccess       use http://host/bucket/key URLs (required by MinIO)
     * @param createBucketIfMissing create the bucket at startup if it does not exist
     */
    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey,
                     boolean pathStyleAccess, boolean createBucketIfMissing) {

        /**
         * Hides the secret key if the settings are ever logged.
         *
         * @return text form without the secret
         */
        @Override
        public String toString() {
            return "S3[endpoint=" + endpoint + ", region=" + region + ", bucket=" + bucket + "]";
        }
    }
}
