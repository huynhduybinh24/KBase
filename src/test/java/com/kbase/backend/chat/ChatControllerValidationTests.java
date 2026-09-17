package com.kbase.backend.chat;

import com.kbase.backend.chat.dto.AskChatRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ChatControllerValidationTests {

    @Test
    void blankAndOversizedMessagesAreRejected() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(validator.validate(new AskChatRequest(" ")).isEmpty());
        assertFalse(validator.validate(new AskChatRequest("x".repeat(10001))).isEmpty());
    }
}
