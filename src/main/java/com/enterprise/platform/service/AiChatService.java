package com.enterprise.platform.service;

import com.enterprise.platform.dto.AiChatRequest;
import com.enterprise.platform.dto.AiChatResponse;
import com.enterprise.platform.dto.RagChatRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;


import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class AiChatService {

    private final RestClient aiRestClient;

    public AiChatService(RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    public AiChatResponse chat(AiChatRequest request) {

        try {
            return aiRestClient.post()
                    .uri("/chat")
                    .body(request)
                    .retrieve()
                    .body(AiChatResponse.class);

        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Python AI 서버에 연결할 수 없습니다."
            );

        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Python AI 서버에서 오류가 발생했습니다."
            );
        }
    }

    // RAG 기반 AI 답변 생성
    public AiChatResponse ragChat(
            String question,
            String context) {

        try {
            RagChatRequest request =
                    new RagChatRequest(question, context);

            return aiRestClient.post()
                    .uri("/rag-chat")
                    .body(request)
                    .retrieve()
                    .body(AiChatResponse.class);

        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Python AI 서버에 연결할 수 없습니다."
            );

        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Python AI 서버에서 RAG 답변 생성 오류가 발생했습니다."
            );
        }
    }
}
