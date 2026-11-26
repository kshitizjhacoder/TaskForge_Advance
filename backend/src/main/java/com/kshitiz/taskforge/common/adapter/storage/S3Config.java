package com.kshitiz.taskforge.common.adapter.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * Creates the AWS SDK {@link S3Client} from {@link StorageProperties}. The same client works
 * with MinIO, Cloudflare R2 and AWS S3; only the endpoint and credentials differ.
 */
@Configuration
public class S3Config {

    /**
     * Builds the S3 client. If no access key is configured, the AWS default credential chain
     * is used (environment, instance role, ...), which is the normal setup on AWS.
     *
     * @param properties storage settings
     * @return a thread-safe S3 client shared by the whole application
     */
    @Bean(destroyMethod = "close")
    public S3Client s3Client(StorageProperties properties) {
        StorageProperties.S3 s3 = properties.s3();
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3.region()))
                .forcePathStyle(s3.pathStyleAccess());
        if (s3.endpoint() != null && !s3.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.endpoint()));
        }
        if (s3.accessKey() != null && !s3.accessKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}
