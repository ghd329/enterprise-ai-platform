package com.enterprise.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {

    @Bean
    public RestClient aiRestClient(
            @Value("${ai.server.url:http://localhost:8001}") String aiServerUrl) {

        return RestClient.builder()
                .baseUrl(aiServerUrl)
                .build();
    }
}