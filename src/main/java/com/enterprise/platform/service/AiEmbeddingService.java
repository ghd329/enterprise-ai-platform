package com.enterprise.platform.service;

import com.enterprise.platform.dto.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class AiEmbeddingService {

    private final RestClient aiRestClient;

    public AiEmbeddingService(RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    public EmbeddingResponse generateEmbedding(String text) {

        try {
            return aiRestClient.post()
                    .uri("/embed")
                    .body(Map.of("text", text))
                    .retrieve()
                    .body(EmbeddingResponse.class);

        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Python AI 서버에 연결할 수 없습니다."
            );

        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Python AI 서버에서 임베딩 오류가 발생했습니다."
            );
        }
    }
}