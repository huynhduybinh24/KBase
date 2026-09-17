package com.kbase.backend.rag.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeterministicDevEmbeddingServiceTests {

    private final DeterministicDevEmbeddingService service =
            new DeterministicDevEmbeddingService(new EmbeddingProperties(
                    "dev", "", "", "test-hash", 32));

    @Test
    void sameInputIsDeterministicWithConfiguredDimensions() {
        EmbeddingResult first = service.embed("authentication security token");
        EmbeddingResult second = service.embed("authentication security token");

        assertEquals(first, second);
        assertEquals(32, first.dimensions());
        assertEquals(32, first.vector().size());
    }

    @Test
    void differentInputProducesDifferentVector() {
        assertNotEquals(service.embed("authentication token").vector(),
                service.embed("project architecture").vector());
    }
}
