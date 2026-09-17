package com.kbase.backend.document;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class DocumentSearchValidationTests {

    @Test
    void rejectsNegativePageNonPositiveSizeAndSizeOverMaximum() throws Exception {
        DocumentController controller = new DocumentController(mock(DocumentService.class));
        Method method = DocumentController.class.getMethod("list", UUID.class, String.class,
                String.class, UUID.class, Instant.class, Instant.class, int.class, int.class,
                String.class, UserDetails.class);
        var executable = Validation.buildDefaultValidatorFactory().getValidator()
                .forExecutables();
        Object[] common = {UUID.randomUUID(), null, null, null, null, null, 0, 20,
                "createdAt,desc", mock(UserDetails.class)};

        Object[] negativePage = common.clone();
        negativePage[6] = -1;
        Object[] zeroSize = common.clone();
        zeroSize[7] = 0;
        Object[] oversized = common.clone();
        oversized[7] = 101;

        assertFalse(executable.validateParameters(controller, method, negativePage).isEmpty());
        assertFalse(executable.validateParameters(controller, method, zeroSize).isEmpty());
        assertFalse(executable.validateParameters(controller, method, oversized).isEmpty());
    }
}
