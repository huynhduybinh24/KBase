package com.kbase.backend.rag.retrieval;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class SemanticSearchValidationTests {

    @Test
    void rejectsBlankQueryAndTopKOutsideSafeRange() throws Exception {
        SemanticSearchController controller = new SemanticSearchController(
                mock(SemanticSearchService.class));
        Method method = SemanticSearchController.class.getMethod("search", UUID.class,
                String.class, int.class, UUID.class, UserDetails.class);
        var validator = Validation.buildDefaultValidatorFactory().getValidator().forExecutables();
        UserDetails principal = mock(UserDetails.class);

        assertFalse(validator.validateParameters(controller, method,
                new Object[]{UUID.randomUUID(), " ", 5, null, principal}).isEmpty());
        assertFalse(validator.validateParameters(controller, method,
                new Object[]{UUID.randomUUID(), "query", 0, null, principal}).isEmpty());
        assertFalse(validator.validateParameters(controller, method,
                new Object[]{UUID.randomUUID(), "query", 21, null, principal}).isEmpty());
    }
}
