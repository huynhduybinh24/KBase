package com.kbase.backend.security;

import com.kbase.backend.config.CorsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class CorsConfigurationTests {
    @Test
    void permitsConfiguredOriginAndOptionsButRejectsOtherOrigin() {
        SecurityConfig config = new SecurityConfig(mock(JwtAuthenticationFilter.class),
                mock(CustomUserDetailsService.class), mock(SecurityErrorWriter.class),
                new CorsProperties(List.of("http://localhost:5173")));
        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertEquals("http://localhost:5173", cors.checkOrigin("http://localhost:5173"));
        assertNull(cors.checkOrigin("https://evil.example"));
        org.junit.jupiter.api.Assertions.assertTrue(
                cors.checkHttpMethod(HttpMethod.OPTIONS).contains(HttpMethod.OPTIONS));
    }
}
