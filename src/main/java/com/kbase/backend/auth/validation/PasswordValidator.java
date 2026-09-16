package com.kbase.backend.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private int minCharacters;

    @Override
    public void initialize(ValidPassword annotation) {
        minCharacters = annotation.minCharacters();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }
        return password.length() >= minCharacters
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
