package com.enterprise.platform.dto;

import java.util.List;

public record MultiDocumentRagQuestionRequest(
        List<Long> documentIds,
        String question,
        Integer topK
) {
}