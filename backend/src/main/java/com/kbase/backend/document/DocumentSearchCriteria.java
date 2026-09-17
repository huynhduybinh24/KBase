package com.kbase.backend.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentSearchCriteria(
        String query,
        String contentType,
        UUID uploadedBy,
        Instant from,
        Instant to
) {
}
