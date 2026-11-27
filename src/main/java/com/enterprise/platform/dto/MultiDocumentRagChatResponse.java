package com.enterprise.platform.dto;

import java.util.List;

public record MultiDocumentRagChatResponse(
        List<Long> documentIds,
        String question,
        String answer,
        String model,
        List<DocumentSearchResult> sources,
        List<DocumentSearchResult> citedSources
) {
}