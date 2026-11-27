package com.enterprise.platform.dto;

public record DocumentSearchResult(
        Long chunkId,
        Long documentId,
        String documentTitle,
        Integer chunkIndex,
        String content,
        double similarity
) {
}