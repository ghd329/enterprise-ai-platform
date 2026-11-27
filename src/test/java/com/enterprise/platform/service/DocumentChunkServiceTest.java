package com.enterprise.platform.service;

import com.enterprise.platform.dto.DocumentSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkServiceTest {

    private final DocumentChunkService service =
            new DocumentChunkService(null, null, null);

    private List<DocumentSearchResult> createSearchResults() {
        return List.of(
                new DocumentSearchResult(
                        7L, 1L, "휴가 규정", 0,
                        "휴가 신청 절차", 0.95
                ),
                new DocumentSearchResult(
                        11L, 2L, "개인정보 규정", 1,
                        "개인정보 보호 절차", 0.90
                ),
                new DocumentSearchResult(
                        6L, 1L, "휴가 규정", 1,
                        "휴가 승인 절차", 0.85
                )
        );
    }

    @Test
    void 실제_검색된_청크의_출처만_추출한다() {
        List<DocumentSearchResult> results =
                service.extractVerifiedSources(
                        "휴가 신청 절차입니다. [출처: 청크 ID 7]",
                        createSearchResults()
                );

        assertEquals(1, results.size());
        assertEquals(7L, results.get(0).chunkId());
    }

    @Test
    void 검색되지_않은_청크_ID는_제외한다() {
        List<DocumentSearchResult> results =
                service.extractVerifiedSources(
                        "[출처: 청크 ID 999]",
                        createSearchResults()
                );

        assertTrue(results.isEmpty());
    }

    @Test
    void 중복된_출처는_한번만_추출한다() {
        List<DocumentSearchResult> results =
                service.extractVerifiedSources(
                        "[출처: 청크 ID 7] [출처: 청크 ID 7]",
                        createSearchResults()
                );

        assertEquals(1, results.size());
    }

    @Test
    void 여러개의_유효한_출처를_모두_추출한다() {
        List<DocumentSearchResult> results =
                service.extractVerifiedSources(
                        "[출처: 청크 ID 7] " +
                                "[출처: 청크 ID 11] " +
                                "[출처: 청크 ID 6]",
                        createSearchResults()
                );

        assertEquals(3, results.size());

        assertEquals(
                List.of(7L, 11L, 6L),
                results.stream()
                        .map(DocumentSearchResult::chunkId)
                        .toList()
        );
    }
}
