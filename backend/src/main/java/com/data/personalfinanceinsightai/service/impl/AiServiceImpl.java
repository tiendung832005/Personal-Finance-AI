package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.ChatCompletionRequest;
import com.data.personalfinanceinsightai.dto.request.ChatMessage;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.service.AiCallLogService;
import com.data.personalfinanceinsightai.service.AiService;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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

    static final String OFF_TOPIC_RESPONSE = "Đây là trợ lý tài chính cá nhân, nên mình chỉ hỗ trợ các câu hỏi liên quan đến tài chính, chi tiêu, thu nhập, ngân sách, tiết kiệm, nợ hoặc đầu tư. Vui lòng đặt lại câu hỏi đúng chủ đề tài chính nhé.";

    private static final Set<String> FINANCE_KEYWORDS = Set.of(
            "tai chinh", "tien", "thu nhap", "luong", "chi tieu", "chi phi", "hoa don",
            "ngan sach", "tiet kiem", "tich luy", "dau tu", "co phieu", "trai phieu",
            "quy dau tu", "lai suat", "vay", "no", "the tin dung", "tin dung", "bao hiem",
            "thue", "tai san", "dong tien", "giao dich", "vi dien tu", "ngan hang",
            "tai khoan", "khoan thu", "khoan chi", "mua nha", "mua xe", "50/30/20",
            "tra gop", "nghi huu",
            "financial", "finance", "money", "income", "salary", "expense", "spending",
            "budget", "saving", "investment", "invest", "debt", "loan", "credit",
            "tax", "bank", "cash flow", "transaction", "portfolio"
    );

    private final AiCallLogService aiCallLogService;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Override
    public String chatWithGemini(User user, ChatCompletionRequest request) {
        // 1. Prepare Messages (Limit to last 6)
        List<ChatMessage> messages = request == null ? null : request.getMessages();
        if (messages == null || messages.isEmpty()) {
            return "Không có tin nhắn nào được gửi.";
        }

        String latestUserMessage = findLatestUserMessage(messages);
        if (latestUserMessage == null || latestUserMessage.isBlank()) {
            return "Không có tin nhắn nào được gửi.";
        }
        if (!isFinanceRelated(latestUserMessage)) {
            return OFF_TOPIC_RESPONSE;
        }

        // 2. Rate Limiting Check (20 calls per day)
        long dailyCount = aiCallLogService.getDailyCallCount(user, "CHATBOT");
        if (dailyCount >= 20) {
            return "Bạn đã hết lượt chat hôm nay (tối đa 20 lượt/ngày). Hãy quay lại vào ngày mai nhé!";
        }

        int startIdx = Math.max(0, messages.size() - 6);
        List<ChatMessage> recentMessages = messages.subList(startIdx, messages.size());

        // 3. Convert to Gemini Format
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessage msg : recentMessages) {
            if (msg == null || msg.getContent() == null || msg.getContent().isBlank()) {
                continue;
            }

            String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "model" : "user";
            
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

        if (contents.isEmpty()) {
            return "Không có tin nhắn nào được gửi.";
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

    private String findLatestUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message != null && !"assistant".equalsIgnoreCase(message.getRole())) {
                return message.getContent();
            }
        }
        return null;
    }

    boolean isFinanceRelated(String text) {
        String normalizedText = normalizeText(text);
        if (normalizedText.isBlank()) {
            return false;
        }

        if (normalizedText.matches(".*\\b\\d+[\\d.,]*\\s*(vnd|vnđ|dong|k|nghin|ngan|trieu|ty|usd|eur)\\b.*")) {
            return true;
        }

        return FINANCE_KEYWORDS.stream().anyMatch(keyword -> containsKeyword(normalizedText, keyword));
    }

    private String normalizeText(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replace('đ', 'd');
    }

    private boolean containsKeyword(String normalizedText, String keyword) {
        return Pattern.compile("(^|\\W)" + Pattern.quote(keyword) + "(\\W|$)")
                .matcher(normalizedText)
                .find();
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

