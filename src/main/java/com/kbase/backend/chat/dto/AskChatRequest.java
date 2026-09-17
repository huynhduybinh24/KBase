package com.kbase.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskChatRequest(
        @NotBlank @Size(max = 10000) String message
) {
}
