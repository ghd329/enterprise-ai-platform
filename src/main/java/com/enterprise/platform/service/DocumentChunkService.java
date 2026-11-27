package com.enterprise.platform.service;

import com.enterprise.platform.dto.EmbeddingResponse;
import com.enterprise.platform.entity.Document;
import com.enterprise.platform.entity.DocumentChunk;
import com.enterprise.platform.repository.DocumentChunkRepository;
import com.enterprise.platform.dto.DocumentSearchResult;
import com.enterprise.platform.dto.RagChatResponse;
import com.enterprise.platform.dto.AiChatResponse;
import com.enterprise.platform.dto.MultiDocumentRagChatResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

@Service
public class DocumentChunkService {

    private static final int CHUNK_SIZE = 600;
    private static final int CHUNK_OVERLAP = 80;
    // 검색 결과 최소 유사도 기준
    private static final double MIN_SIMILARITY = 0.55;
    private static final Pattern SOURCE_PATTERN =
            Pattern.compile(
                    "\\[출처:\\s*청크 ID\\s*(\\d+)\\]"
            );

    private final DocumentChunkRepository documentChunkRepository;
    private final AiEmbeddingService aiEmbeddingService;
    private final AiChatService aiChatService;

    public DocumentChunkService(
            DocumentChunkRepository documentChunkRepository,
            AiEmbeddingService aiEmbeddingService,
            AiChatService aiChatService) {

        this.documentChunkRepository = documentChunkRepository;
        this.aiEmbeddingService = aiEmbeddingService;
        this.aiChatService = aiChatService;
    }

    // 문서를 일정한 크기로 분할
    @Transactional
    public void createChunks(Document document) {

        String text = document.getContent();

        if (text == null || text.isBlank()) {
            return;
        }

        documentChunkRepository.deleteByDocumentId(document.getId());

        List<DocumentChunk> chunks = new ArrayList<>();

        int start = 0;
        int chunkIndex = 0;

        while (start < text.length()) {

            int end = Math.min(start + CHUNK_SIZE, text.length());

            String chunkContent = text.substring(start, end).trim();

            if (!chunkContent.isBlank()) {
                chunks.add(
                        new DocumentChunk(document, chunkIndex, chunkContent)
                );

                chunkIndex++;
            }

            if (end == text.length()) {
                break;
            }

            start = end - CHUNK_OVERLAP;
        }

        documentChunkRepository.saveAll(chunks);
    }

    // 문서 청크 조회
    @Transactional(readOnly = true)
    public List<DocumentChunk> getChunks(Long documentId) {

        return documentChunkRepository
                .findByDocumentIdOrderByChunkIndexAsc(documentId);
    }

    // 문서 청크의 임베딩 생성 및 저장
    @Transactional
    public int createEmbeddings(Long documentId) {

        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentIdOrderByChunkIndexAsc(documentId);

        // 청크가 존재하는지 먼저 확인
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "해당 문서에 저장된 청크가 없습니다. documentId: " + documentId
            );
        }

        for (DocumentChunk chunk : chunks) {

            EmbeddingResponse response =
                    aiEmbeddingService.generateEmbedding(chunk.getContent());

            if (response == null
                    || response.embedding() == null
                    || response.embedding().isEmpty()) {

                throw new IllegalStateException(
                        "임베딩 생성 결과가 비어 있습니다. 청크 ID: " + chunk.getId()
                );
            }

            if (response.dimension() != response.embedding().size()) {
                throw new IllegalStateException(
                        "임베딩 차원 정보가 일치하지 않습니다."
                );
            }

            chunk.setEmbedding(response.embedding());
        }

        documentChunkRepository.saveAll(chunks);

        return chunks.size();
    }

    // 질문과 문서 청크의 유사도 검색
    @Transactional(readOnly = true)
    public List<DocumentChunk> searchSimilarChunks(
            Long documentId,
            String question,
            int topK) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("검색 질문을 입력해주세요.");
        }

        if (topK < 1) {
            throw new IllegalArgumentException("topK는 1 이상이어야 합니다.");
        }

        // 사용자 질문을 임베딩 벡터로 변환
        EmbeddingResponse questionResponse =
                aiEmbeddingService.generateEmbedding(question);

        List<Double> questionVector = questionResponse.embedding();

        // 해당 문서의 청크 조회
        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentIdOrderByChunkIndexAsc(documentId);

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("검색할 문서 청크가 없습니다.");
        }

        // 임베딩이 저장된 청크만 대상으로 유사도 계산
        return chunks.stream()
                .filter(chunk -> chunk.getEmbedding() != null)
                .sorted(Comparator.comparingDouble(
                        (DocumentChunk chunk) ->
                                cosineSimilarity(
                                        questionVector,
                                        chunk.getEmbedding()
                                )
                ).reversed())
                .limit(topK)
                .toList();
    }

    // 질문과 문서 청크의 유사도를 점수와 함께 검색
    @Transactional(readOnly = true)
    public List<DocumentSearchResult> searchSimilarChunksWithScore(
            Long documentId,
            String question,
            int topK) {

        // 질문 검증
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("검색 질문을 입력해주세요.");
        }

        // 검색 개수 검증
        if (topK < 1) {
            throw new IllegalArgumentException("topK는 1 이상이어야 합니다.");
        }

        // 질문을 임베딩 벡터로 변환
        EmbeddingResponse questionResponse =
                aiEmbeddingService.generateEmbedding(question);

        if (questionResponse == null
                || questionResponse.embedding() == null
                || questionResponse.embedding().isEmpty()) {
            throw new IllegalStateException("질문 임베딩 생성에 실패했습니다.");
        }

        List<Double> questionVector = questionResponse.embedding();

        // 해당 문서의 청크 조회
        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentIdOrderByChunkIndexAsc(documentId);

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("검색할 문서 청크가 없습니다.");
        }

        // 유사도 계산 후 기준 점수 이상의 청크만 선택
        return chunks.stream()
                .filter(chunk -> chunk.getEmbedding() != null)
                .map(chunk -> new DocumentSearchResult(
                        chunk.getId(),
                        documentId,
                        chunk.getDocument().getTitle(),
                        chunk.getChunkIndex(),
                        chunk.getContent(),
                        cosineSimilarity(questionVector, chunk.getEmbedding())
                ))
                .filter(result -> result.similarity() >= MIN_SIMILARITY)
                .sorted(Comparator.comparingDouble(
                        DocumentSearchResult::similarity
                ).reversed())
                // 최대 검색 개수 제한
                .limit(topK)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentSearchResult> searchSimilarChunksWithScore(
            List<Long> documentIds,
            String question,
            int topK) {

        if (documentIds == null || documentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "검색할 문서 ID를 입력해주세요."
            );
        }

        if (documentIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException(
                    "올바른 문서 ID를 입력해주세요."
            );
        }

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "검색 질문을 입력해주세요."
            );
        }

        if (topK < 1) {
            throw new IllegalArgumentException(
                    "topK는 1 이상이어야 합니다."
            );
        }

        // 질문을 임베딩 벡터로 변환
        EmbeddingResponse questionResponse =
                aiEmbeddingService.generateEmbedding(question);

        if (questionResponse == null
                || questionResponse.embedding() == null
                || questionResponse.embedding().isEmpty()) {

            throw new IllegalStateException(
                    "질문 임베딩 생성에 실패했습니다."
            );
        }

        List<Double> questionVector =
                questionResponse.embedding();

        // 여러 문서에 포함된 모든 청크 조회
        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentIdInOrderByDocumentIdAscChunkIndexAsc(
                                documentIds
                        );

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "검색할 문서 청크가 없습니다."
            );
        }

        // 청크별 유사도 계산 및 검색 결과 반환
        return chunks.stream()
                .filter(chunk -> chunk.getEmbedding() != null)
                .map(chunk -> new DocumentSearchResult(
                        chunk.getId(),
                        chunk.getDocument().getId(),
                        chunk.getDocument().getTitle(),
                        chunk.getChunkIndex(),
                        chunk.getContent(),
                        cosineSimilarity(
                                questionVector,
                                chunk.getEmbedding()
                        )
                ))
                .filter(result ->
                        result.similarity() >= MIN_SIMILARITY
                )
                .sorted(Comparator.comparingDouble(
                        DocumentSearchResult::similarity
                ).reversed())
                .limit(topK)
                .toList();
    }

    public MultiDocumentRagChatResponse generateMultiDocumentRagAnswer(
            List<Long> documentIds,
            String question,
            int topK) {

        // 여러 문서에서 관련 내용 검색
        List<DocumentSearchResult> searchResults =
                searchSimilarChunksWithScore(
                        documentIds,
                        question,
                        topK
                );

        if (searchResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "질문과 관련된 문서 내용을 찾을 수 없습니다."
            );
        }

        // 검색된 문서 내용을 하나의 문맥으로 결합

        String context = searchResults.stream()
                .map(result ->
                        "[청크 ID: " + result.chunkId()
                                + ", 문서 ID: " + result.documentId()
                                + ", 문서 제목: " + result.documentTitle()
                                + ", 청크 번호: " + result.chunkIndex()
                                + "]\n"
                                + result.content()
                )
                .collect(
                        java.util.stream.Collectors.joining("\n\n")
                );

        // Python AI 서버에 질문과 문맥 전달
        AiChatResponse aiResponse =
                aiChatService.ragChat(question, context);

        // 여러 문서의 검색 결과와 AI 답변 반환
        List<DocumentSearchResult> citedSources =
                extractVerifiedSources(
                        aiResponse.getAnswer(),
                        searchResults
                );

        return new MultiDocumentRagChatResponse(
                documentIds,
                question,
                aiResponse.getAnswer(),
                aiResponse.getModel(),
                searchResults,
                citedSources
        );
    }

    // 문서 검색 결과를 활용한 RAG 답변 생성
    public RagChatResponse generateRagAnswer(
            Long documentId,
            String question,
            int topK) {

        // 질문과 관련된 문서 청크 검색
        List<DocumentSearchResult> searchResults =
                searchSimilarChunksWithScore(
                        documentId,
                        question,
                        topK
                );

        if (searchResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "질문과 관련된 문서 내용을 찾을 수 없습니다. " +
                            "질문을 구체적으로 입력하거나 다른 질문을 시도해주세요."
            );
        }
        // 검색된 청크 내용을 하나의 문맥으로 합치기
        String context = searchResults.stream()
                .map(DocumentSearchResult::content)
                .collect(java.util.stream.Collectors.joining("\n\n"));

        // Python AI 서버에 질문과 문맥 전달
        AiChatResponse aiResponse =
                aiChatService.ragChat(question, context);

        return new RagChatResponse(
                documentId,
                question,
                aiResponse.getAnswer(),
                aiResponse.getModel(),
                searchResults
        );
    }

    // 코사인 유사도 계산
    private double cosineSimilarity(
            List<Double> vectorA,
            List<Double> vectorB) {

        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("벡터 차원이 서로 다릅니다.");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {

            dotProduct += vectorA.get(i) * vectorB.get(i);

            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }



    private List<DocumentSearchResult> extractVerifiedSources(
            String answer,
            List<DocumentSearchResult> searchResults
    ) {
        List<DocumentSearchResult> verifiedSources =
                new ArrayList<>();

        Matcher matcher = SOURCE_PATTERN.matcher(answer);

        while (matcher.find()) {

            Long chunkId =
                    Long.parseLong(matcher.group(1));

            searchResults.stream()
                    .filter(result ->
                            result.chunkId().equals(chunkId)
                    )
                    .findFirst()
                    .ifPresent(result -> {
                        if (!verifiedSources.contains(result)) {
                            verifiedSources.add(result);
                        }
                    });
        }

        return verifiedSources;
    }


}