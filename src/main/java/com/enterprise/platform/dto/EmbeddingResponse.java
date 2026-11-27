package com.enterprise.platform.dto;

import java.util.List;

public record EmbeddingResponse(
        String model,
        int dimension,
        List<Double> embedding
) {
}