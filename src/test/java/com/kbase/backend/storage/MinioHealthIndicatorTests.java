package com.kbase.backend.storage;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinioHealthIndicatorTests {
    @Test
    void reportsUpWhenMinioRespondsEvenBeforeBucketCreation() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any())).thenReturn(false);
        var indicator = new MinioHealthIndicator(client,
                new StorageProperties("http://localhost:9000", "key", "secret", "bucket", 10));
        assertEquals("UP", indicator.health().getStatus().getCode());
    }

    @Test
    void reportsDownWithoutExposingProviderException() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any())).thenThrow(new RuntimeException("secret provider detail"));
        var indicator = new MinioHealthIndicator(client,
                new StorageProperties("http://localhost:9000", "key", "secret", "bucket", 10));
        var health = indicator.health();
        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals(0, health.getDetails().size());
    }
}
