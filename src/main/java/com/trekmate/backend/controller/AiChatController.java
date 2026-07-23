package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.AiChatRequest;
import com.trekmate.backend.dto.response.AiChatResponse;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Assistant", description = "Chatbot AI tư vấn tour, thời tiết, thiết bị")
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    @Operation(summary = "Gửi tin nhắn cho AI", description = "Gửi tin nhắn và nhận câu trả lời từ AI Trekking Assistant")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        log.info("Received chat request: {}", request.getMessage());
        AiChatResponse response = aiChatService.processChat(request);
        
        return ApiResponse.<AiChatResponse>builder()
                .code(200)
                .message("Success")
                .data(response)
                .build();
    }
}
