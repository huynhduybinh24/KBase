package com.kbase.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.chat")
public record ChatProtectionProperties(
        @Positive int maxMessageChars,
        @Positive int rateLimitRequests,
        @Positive int rateLimitWindowSeconds
) {
}
