package com.enterprise.platform.dto;

public record RagQuestionRequest(
        String question,
        Integer topK
) {
}