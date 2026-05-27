package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Wrapper service for Google Gemini API.
 * Supports system prompt, generation config, retry with exponential backoff on 429.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final GeminiProperties props;

    private WebClient webClient;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    /**
     * Gửi request tới Gemini API với system prompt và user message.
     *
     * @param systemPrompt chỉ dẫn hệ thống cho AI
     * @param userMessage  nội dung người dùng gửi
     * @return text response từ AI, hoặc null nếu lỗi
     */
    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, 100);
    }

    /**
     * Gửi request tới Gemini API với cấu hình maxTokens tùy chỉnh.
     */
    public String chat(String systemPrompt, String userMessage, int maxTokens) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            log.error("Gemini API key is not configured. Set GEMINI_API_KEY environment variable.");
            return null;
        }

        String[] modelList = props.getFallbackModels().split(",");
        
        for (String modelName : modelList) {
            String model = modelName.trim();
            if (model.isEmpty()) continue;

            String uri = String.format(
                    "/v1beta/models/%s:generateContent?key=%s",
                    model, props.getApiKey()
            );

            Map<String, Object> requestBody = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt))
                    ),
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", userMessage))
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "maxOutputTokens", maxTokens
                    )
            );

            for (int attempt = 1; attempt <= props.getMaxRetries(); attempt++) {
                try {
                    long startMs = System.currentTimeMillis();

                    String responseBody = webClient.post()
                            .uri(uri)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                            .block();

                    long latency = System.currentTimeMillis() - startMs;

                    if (responseBody == null) {
                        log.error("Gemini model {} returned null response body", model);
                        break; // switch model
                    }

                    JsonNode node = objectMapper.readTree(responseBody);

                    JsonNode candidatesNode = node.path("candidates");
                    if (candidatesNode.isMissingNode() || !candidatesNode.isArray() || candidatesNode.isEmpty()) {
                        log.error("Gemini model {} response missing candidates: {}", model, responseBody);
                        break; // switch model
                    }

                    JsonNode firstCandidate = candidatesNode.get(0);
                    JsonNode contentNode = firstCandidate.path("content");
                    if (contentNode.isMissingNode() || contentNode.isNull()) {
                        log.error("Gemini model {} candidate zero missing content", model);
                        break; // switch model
                    }
                    
                    JsonNode partsNode = contentNode.path("parts");
                    if (partsNode.isMissingNode() || !partsNode.isArray() || partsNode.isEmpty()) {
                        log.error("Gemini model {} parts missing", model);
                        break; // switch model
                    }
                    
                    String text = partsNode.get(0).path("text").asText("").trim();
                    int totalTokens = node.path("usageMetadata").path("totalTokenCount").asInt(0);

                    log.info("Gemini OK: model={}, tokens={}, latency={}ms", model, totalTokens, latency);
                    return text;

                } catch (WebClientResponseException ex) {
                    if (ex.getStatusCode().value() == 429) {
                        if (attempt < props.getMaxRetries()) {
                            log.warn("Gemini model {} 429 limited, retry {}/{}", model, attempt, props.getMaxRetries());
                            try { Thread.sleep(1500L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
                            continue;
                        } else {
                            log.warn("Gemini model {} quota exhausted, failing over...", model);
                            break; 
                        }
                    }
                    log.error("Gemini model {} HTTP error {}: {}", model, ex.getStatusCode().value(), ex.getResponseBodyAsString());
                    break; 
                } catch (Exception e) {
                    log.error("Gemini model {} failed: {}", model, e.getMessage());
                    break; 
                }
            }
        }
        return null;
    }
}
