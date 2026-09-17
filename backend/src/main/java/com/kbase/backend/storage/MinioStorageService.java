package com.kbase.backend.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MinioStorageService implements StorageService {

    private final MinioClient client;
    private final StorageProperties properties;
    private volatile boolean bucketReady;

    public MinioStorageService(MinioClient client, StorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void upload(String storageKey, InputStream content, long size, String contentType) {
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storageKey)
                    .stream(content, size, -1L)
                    .contentType(contentType)
                    .build());
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storageKey)
                    .build());
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storageKey)
                    .build());
            return true;
        } catch (ErrorResponseException exception) {
            if (exception.errorResponse() != null
                    && "NoSuchKey".equals(exception.errorResponse().code())) {
                return false;
            }
            throw unavailable(exception);
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storageKey)
                    .build());
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        boolean exists = client.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
        }
        bucketReady = true;
    }

    private StorageException unavailable(Exception cause) {
        return new StorageException("Object storage is unavailable", cause);
    }
}
