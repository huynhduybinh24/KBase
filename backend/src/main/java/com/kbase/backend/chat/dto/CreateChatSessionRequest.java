package com.kbase.backend.chat.dto;

import jakarta.validation.constraints.Size;

public record CreateChatSessionRequest(@Size(max = 200) String title) {
}
