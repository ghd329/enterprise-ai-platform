package com.enterprise.platform.controller;

import com.enterprise.platform.dto.AiChatRequest;
import com.enterprise.platform.dto.AiChatResponse;
import com.enterprise.platform.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(
            @Valid @RequestBody AiChatRequest request) {

        return aiChatService.chat(request);
    }
}
