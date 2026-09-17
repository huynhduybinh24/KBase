package com.kbase.backend.storage;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("minio")
public class MinioHealthIndicator implements HealthIndicator {
    private final MinioClient client;
    private final StorageProperties properties;

    public MinioHealthIndicator(MinioClient client, StorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public Health health() {
        try {
            client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build());
            return Health.up().build();
        } catch (Exception exception) {
            return Health.down().build();
        }
    }
}
