package com.kbase.backend.document.extraction;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class PlainTextExtractor implements TextExtractor {

    private static final Set<String> TYPES = Set.of("text/plain", "text/markdown");

    @Override
    public boolean supports(String contentType) {
        return TYPES.contains(contentType);
    }

    @Override
    public String extract(InputStream input) throws Exception {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
}
