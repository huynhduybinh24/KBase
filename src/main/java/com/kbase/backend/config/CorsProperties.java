package com.kbase.backend.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
    @AssertTrue(message = "CORS origins must be explicit and must not contain wildcards")
    public boolean isExplicitAllowlist() {
        return allowedOrigins != null && allowedOrigins.stream()
                .allMatch(origin -> origin != null && !origin.isBlank() && !origin.contains("*"));
    }
}
