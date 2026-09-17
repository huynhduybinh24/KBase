package com.kbase.backend.chat;

import com.kbase.backend.config.ChatProtectionProperties;
import com.kbase.backend.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatRateLimiterTests {
    @Test
    void permitsConfiguredRequestsThenRejectsExcessPerUser() {
        ChatRateLimiter limiter = new ChatRateLimiter(
                new ChatProtectionProperties(4000, 2, 60),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertDoesNotThrow(() -> limiter.check(first));
        assertDoesNotThrow(() -> limiter.check(first));
        assertThrows(RateLimitExceededException.class, () -> limiter.check(first));
        assertDoesNotThrow(() -> limiter.check(second));
    }
}
