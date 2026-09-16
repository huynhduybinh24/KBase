package com.kbase.backend.document.extraction;

import java.io.InputStream;

public interface TextExtractor {

    boolean supports(String contentType);

    String extract(InputStream input) throws Exception;
}
