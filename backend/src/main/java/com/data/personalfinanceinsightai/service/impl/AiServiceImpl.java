package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.ChatCompletionRequest;
import com.data.personalfinanceinsightai.dto.request.ChatMessage;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.service.AiCallLogService;
import com.data.personalfinanceinsightai.service.AiService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiCallLogService aiCallLogService;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Override
    public String chatWithGemini(User user, ChatCompletionRequest request) {
        // 1. Rate Limiting Check (20 calls per day)
        long dailyCount = aiCallLogService.getDailyCallCount(user, "CHATBOT");
        if (dailyCount >= 20) {
            return "Bạn đã hết lượt chat hôm nay (tối đa 20 lượt/ngày). Hãy quay lại vào ngày mai nhé!";
        }

        // 2. Prepare Messages (Limit to last 6)
        List<ChatMessage> messages = request.getMessages();
        if (messages == null || messages.isEmpty()) {
            return "Không có tin nhắn nào được gửi.";
        }

        int startIdx = Math.max(0, messages.size() - 6);
        List<ChatMessage> recentMessages = messages.subList(startIdx, messages.size());

        // 3. Convert to Gemini Format
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessage msg : recentMessages) {
            String role = msg.getRole().equalsIgnoreCase("assistant") ? "model" : "user";
            
            // Gemini doesn't allow two messages with the same role in a row.
            // Also, the first message in 'contents' MUST be 'user' (optional, but recommended).
            if (!contents.isEmpty()) {
                String lastRole = (String) contents.get(contents.size() - 1).get("role");
                if (lastRole.equals(role)) {
                    continue; // Skip if same role in a row to avoid error
                }
            } else if (role.equals("model")) {
                continue; // Skip if first message is a model/assistant message
            }

            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", msg.getContent()))
            ));
        }

        // 4. Call Gemini
        String uri = String.format("/v1beta/models/%s:generateContent?key=%s", model, apiKey);
        Map<String, Object> payload = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", "Bạn là một chuyên gia tư vấn tài chính cá nhân. Hãy trả lời các câu hỏi sau đây một cách chuyên nghiệp, hữu ích và thân thiện."))),
                "contents", contents
        );


        long startTime = System.currentTimeMillis();
        try {
            GeminiGenerateResponse response = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build()
                    .post()
                    .uri(uri)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(GeminiGenerateResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            long latency = System.currentTimeMillis() - startTime;

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                aiCallLogService.logFailure(user, "CHATBOT", "Empty response from Gemini");
                return "Gemini không phản hồi. Vui lòng thử lại sau.";
            }

            String aiText = response.candidates().get(0).content().parts().get(0).text().trim();
            
            // Log success with token estimation if available
            int pTokens = response.usageMetadata() != null ? response.usageMetadata().promptTokenCount() : 0;
            int cTokens = response.usageMetadata() != null ? response.usageMetadata().candidatesTokenCount() : 0;
            
            aiCallLogService.logSuccess(user, "CHATBOT", model, pTokens, cTokens, (int) latency);
            
            return aiText;
        } catch (Exception ex) {
            aiCallLogService.logFailure(user, "CHATBOT", ex.getMessage());
            return "Có lỗi xảy ra khi kết nối với AI. Vui lòng thử lại.";
        }
    }

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

    private record GeminiGenerateResponse(List<GeminiCandidate> candidates, GeminiUsage usageMetadata) {
    }

    private record GeminiUsage(int promptTokenCount, int candidatesTokenCount, int totalTokenCount) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }

    private record GeminiContent(List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }
}

