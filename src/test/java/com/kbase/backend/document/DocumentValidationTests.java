package com.kbase.backend.document;

import com.kbase.backend.document.dto.CreateDocumentRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidMetadata() {
        assertTrue(validator.validate(new CreateDocumentRequest(
                "Guide",
                null,
                "guide.pdf",
                "projects/key.pdf",
                "application/pdf",
                1
        )).isEmpty());
    }

    @Test
    void rejectsBlankFieldsAndNonPositiveSize() {
        assertFalse(validator.validate(new CreateDocumentRequest(
                " ",
                null,
                " ",
                " ",
                " ",
                0
        )).isEmpty());
    }
}
