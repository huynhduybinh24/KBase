package com.kbase.backend.document.extraction;

import com.kbase.backend.document.Document;
import com.kbase.backend.document.extraction.dto.DocumentContentResponse;

import java.util.UUID;

public interface DocumentExtractionService {

    void initializeAndExtract(Document document);

    DocumentContentResponse get(UUID documentId);

    DocumentContentResponse retry(Document document);
}
