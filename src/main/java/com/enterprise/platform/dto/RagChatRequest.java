package com.enterprise.platform.dto;

public record RagChatRequest(
        String question,
        String context
) {
}