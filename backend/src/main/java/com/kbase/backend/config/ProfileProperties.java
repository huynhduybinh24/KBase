package com.kbase.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.profile")
@Validated
public record ProfileProperties(@Positive long avatarMaxFileSize) {
}
