package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.service.AiService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Service
public class AiServiceImpl implements AiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Override
    public String testGeminiConnection() {
        try {
            String response = askGemini("Hello, are you working?");
            return "Gemini connection successful. Response: " + response;
        } catch (Exception e) {
            return "Gemini connection failed. Error: " + e.getMessage();
        }
    }

    @Override
    public String askGemini(String question) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Gemini API key is missing. Please set GEMINI_API_KEY.";
        }

        String prompt = question == null ? "" : question.trim();
        if (prompt.isEmpty()) {
            return "Question must not be empty.";
        }

        String uri = String.format("/v1beta/models/%s:generateContent?key=%s", model, apiKey);
        Map<String, Object> payload = Map.of(
                "contents",
                List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        try {
            GeminiGenerateResponse response = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build()
                    .post()
                    .uri(uri)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(GeminiGenerateResponse.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            if (response == null
                    || response.candidates() == null
                    || response.candidates().isEmpty()
                    || response.candidates().get(0).content() == null
                    || response.candidates().get(0).content().parts() == null
                    || response.candidates().get(0).content().parts().isEmpty()
                    || response.candidates().get(0).content().parts().get(0).text() == null) {
                return "Gemini returned no usable response.";
            }

            return response.candidates().get(0).content().parts().get(0).text().trim();
        } catch (WebClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 401 || status.value() == 403) {
                return "Gemini authorization failed. Check GEMINI_API_KEY.";
            }
            if (status.value() == 429) {
                return "Gemini rate limit exceeded. Please retry later.";
            }
            return "Gemini API error (" + status.value() + ").";
        } catch (WebClientRequestException ex) {
            return "Gemini network/timeout error. Please retry.";
        } catch (Exception ex) {
            return "Unexpected Gemini integration error.";
        }
    }

    private record GeminiGenerateResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }

    private record GeminiContent(List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }
}

