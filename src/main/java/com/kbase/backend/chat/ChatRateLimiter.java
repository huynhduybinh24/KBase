package com.kbase.backend.chat;

import com.kbase.backend.config.ChatProtectionProperties;
import com.kbase.backend.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatRateLimiter {
    private final ChatProtectionProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<UUID, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public ChatRateLimiter(ChatProtectionProperties properties) {
        this(properties, Clock.systemUTC());
    }

    ChatRateLimiter(ChatProtectionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void check(UUID userId) {
        Instant now = clock.instant();
        Window result = windows.compute(userId, (ignored, current) -> {
            if (current == null || !now.isBefore(current.resetAt())) {
                return new Window(1, now.plusSeconds(properties.rateLimitWindowSeconds()));
            }
            return new Window(current.requests() + 1, current.resetAt());
        });
        if (result.requests() > properties.rateLimitRequests()) {
            long retry = Math.max(1, result.resetAt().getEpochSecond() - now.getEpochSecond());
            throw new RateLimitExceededException(retry);
        }
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().resetAt()));
        }
    }

    private record Window(int requests, Instant resetAt) {
    }
}
