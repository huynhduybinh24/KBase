package com.kbase.backend.document.chunk;

import com.kbase.backend.document.Document;
import com.kbase.backend.document.extraction.DocumentContent;
import com.kbase.backend.document.extraction.dto.DocumentContentResponse;

public interface DocumentChunkService {

    void indexAfterExtraction(Document document, DocumentContent content);

    DocumentContentResponse reindex(Document document);
}
