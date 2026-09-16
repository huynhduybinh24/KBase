package com.kbase.backend.storage;

import java.io.InputStream;

public interface StorageService {

    void upload(String storageKey, InputStream content, long size, String contentType);

    InputStream download(String storageKey);

    boolean exists(String storageKey);

    void delete(String storageKey);
}
