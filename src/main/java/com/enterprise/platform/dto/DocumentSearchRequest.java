package com.enterprise.platform.dto;

public record DocumentSearchRequest(
        String question,
        Integer topK
) {
}