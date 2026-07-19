package com.trekmate.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trekmate.backend.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiClient {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public String generateContent(String systemPrompt, String userMessage) {
        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            log.warn("Gemini API key is not configured. Returning fallback response.");
            return "Xin lỗi, tính năng AI hiện tại chưa được cấu hình. Vui lòng liên hệ quản trị viên.";
        }

        try {
            String url = String.format(GEMINI_API_URL, geminiProperties.getModel(), geminiProperties.getApiKey());

            // Build request payload
            ObjectNode root = objectMapper.createObjectNode();

            // System instruction
            ObjectNode systemInstruction = root.putObject("systemInstruction");
            ArrayNode sysParts = systemInstruction.putArray("parts");
            sysParts.addObject().put("text", systemPrompt);

            // Contents
            ArrayNode contents = root.putArray("contents");
            ObjectNode contentObj = contents.addObject();
            contentObj.put("role", "user");
            ArrayNode parts = contentObj.putArray("parts");
            parts.addObject().put("text", userMessage);

            // Generation Config (optional, to force structured JSON output if needed, but we just need text here)
            // ObjectNode genConfig = root.putObject("generationConfig");
            // genConfig.put("responseMimeType", "application/json");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(root), headers);

            log.debug("Sending request to Gemini API...");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            
            // Extract the text response
            JsonNode candidates = responseJson.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode partsNode = firstCandidate.path("content").path("parts");
                if (partsNode.isArray() && partsNode.size() > 0) {
                    return partsNode.get(0).path("text").asText();
                }
            }

            return "Xin lỗi, tôi không thể tạo câu trả lời lúc này.";

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            return "Đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau.";
        }
    }
}
