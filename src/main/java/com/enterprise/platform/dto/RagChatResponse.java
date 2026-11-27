package com.enterprise.platform.dto;

import java.util.List;

public record RagChatResponse(
        Long documentId,
        String question,
        String answer,
        String model,
        List<DocumentSearchResult> sources
) {
}