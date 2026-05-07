package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.service.AiService;
import com.data.personalfinanceinsightai.dto.request.ChatCompletionRequest;
import com.data.personalfinanceinsightai.dto.response.ChatCompletionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Service
public class AiServiceImpl implements AiService {

    @Value("${openai.api-key}")
    private String apiKey;

    private final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final String MODEL = "gpt-3.5-turbo";

    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = WebClient.create();
        }
        return webClient;
    }

    @Override
    public String testOpenAiConnection() {
        try {
            String response = askChatGPT("Hello, are you working?");
            return "✅ OpenAI connection successful! Response: " + response;
        } catch (Exception e) {
            return "❌ OpenAI connection failed! Error: " + e.getMessage();
        }
    }

    @Override
    public String askChatGPT(String question) {
        try {
            // Tạo request
            ChatCompletionRequest.ChatMessage userMessage = new ChatCompletionRequest.ChatMessage("user", question);
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(MODEL)
                    .messages(Arrays.asList(userMessage))
                    .max_tokens(500)
                    .temperature(0.7)
                    .build();

            // Gửi request và lấy response
            ChatCompletionResponse response = getWebClient()
                    .post()
                    .uri(OPENAI_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block();

            if (response != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            } else {
                return "No response from OpenAI";
            }
        } catch (Exception e) {
            return "Error calling OpenAI API: " + e.getMessage();
        }
    }
}

