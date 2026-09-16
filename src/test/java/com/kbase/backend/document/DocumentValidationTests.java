package com.kbase.backend.document;

import com.kbase.backend.exception.BadRequestException;
import com.kbase.backend.exception.FileTooLargeException;
import com.kbase.backend.storage.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentValidationTests {

    private final DocumentFileValidator validator = new DocumentFileValidator(
            new StorageProperties("http://localhost:9000", "key", "secret", "bucket", 10));

    @Test
    void acceptsAllowedFileAndSanitizesItsName() {
        var result = validator.validate(file("my guide.pdf", "application/pdf", new byte[]{1}));
        assertEquals("my_guide.pdf", result.safeFileName());
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(BadRequestException.class,
                () -> validator.validate(file("guide.pdf", "application/pdf", new byte[0])));
    }

    @Test
    void rejectsOversizedFile() {
        assertThrows(FileTooLargeException.class,
                () -> validator.validate(file("guide.pdf", "application/pdf", new byte[11])));
    }

    @Test
    void rejectsPathTraversalAndUnsupportedContentType() {
        assertThrows(BadRequestException.class,
                () -> validator.validate(file("../guide.pdf", "application/pdf", new byte[]{1})));
        assertThrows(BadRequestException.class,
                () -> validator.validate(file("guide.exe", "application/octet-stream", new byte[]{1})));
    }

    private MockMultipartFile file(String name, String type, byte[] content) {
        return new MockMultipartFile("file", name, type, content);
    }
}
