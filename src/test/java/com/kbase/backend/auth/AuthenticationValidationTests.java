package com.kbase.backend.auth;

import com.kbase.backend.auth.dto.LoginRequest;
import com.kbase.backend.auth.dto.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationValidationTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidRegistration() {
        assertTrue(validator.validate(
                new RegisterRequest("person@example.com", "password123")
        ).isEmpty());
    }

    @Test
    void rejectsInvalidEmailAndShortPassword() {
        assertFalse(validator.validate(
                new RegisterRequest("not-an-email", "short")
        ).isEmpty());
    }

    @Test
    void rejectsPasswordBeyondBcryptUtf8ByteLimit() {
        String unicodePassword = "😀".repeat(19);

        assertFalse(validator.validate(
                new RegisterRequest("person@example.com", unicodePassword)
        ).isEmpty());
        assertFalse(validator.validate(
                new LoginRequest("person@example.com", unicodePassword)
        ).isEmpty());
    }
}
