package com.kbase.backend.config;

import com.kbase.backend.rag.chunking.RagProperties;
import com.kbase.backend.storage.StorageProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class HardeningConfigurationValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidChunkSizeAndOverlap() {
        assertFalse(validator.validate(new RagProperties(0, 0)).isEmpty());
        assertFalse(validator.validate(new RagProperties(10, 10)).isEmpty());
    }

    @Test
    void rejectsInvalidUploadLimitAndRateSettings() {
        assertFalse(validator.validate(new StorageProperties(
                "http://localhost:9000", "key", "secret", "bucket", 0)).isEmpty());
        assertFalse(validator.validate(new ChatProtectionProperties(0, 0, 0)).isEmpty());
    }

    @Test
    void rejectsWildcardCorsOrigin() {
        assertFalse(validator.validate(new CorsProperties(java.util.List.of("*"))).isEmpty());
    }
}
