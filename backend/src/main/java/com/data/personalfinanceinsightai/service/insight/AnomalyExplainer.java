package com.data.personalfinanceinsightai.service.insight;

import com.data.personalfinanceinsightai.entity.SpendingAnomaly;
import com.data.personalfinanceinsightai.repository.SpendingAnomalyRepository;
import com.data.personalfinanceinsightai.service.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * T11 — AnomalyExplainer: Sử dụng Gemini để sinh lời giải thích thân thiện cho người dùng
 * khi phát hiện có chi tiêu bất thường.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyExplainer {

    private final GeminiClient geminiClient;
    private final SpendingAnomalyRepository anomalyRepository;

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý tài chính AI thông minh.
            Dựa trên thông tin về một giao dịch chi tiêu bất thường, hãy viết một lời giải thích hoặc lời nhắc ngắn (1-2 câu) bằng tiếng Việt.
            
            Yêu cầu:
            - Thân thiện, nhẹ nhàng, không phán xét (kể cả khi chi tiêu quá tay).
            - Giải thích cho người dùng biết tại sao hệ thống lại gắn cờ giao dịch này.
            - Nếu là khoản chi lớn, có thể gợi ý người dùng xem xét lại ngân sách.
            """;

    /**
     * Gọi AI giải thích bất thường.
     * 
     * @param anomaly Bản ghi bất thường cần giải thích
     * @param technicalReason Lý do thô được sinh ra từ quy tắc (Rule-based)
     */
    @Async
    public void explainAnomaly(SpendingAnomaly anomaly, String technicalReason) {
        if (anomaly.getExplanation() != null) return; // Đã có giải thích thì bỏ qua

        String description = anomaly.getTransaction().getDescription();
        String amount = anomaly.getTransaction().getAmount().toString();
        
        String userMessage = String.format(
                "Phát hiện bất thường: Giao dịch '%s' chi %sđ.\nLý do hệ thống: %s.\n\nHãy viết một câu giải thích/nhắc nhở thân thiện cho tôi.",
                description, amount, technicalReason
        );

        log.debug("Requesting AI explanation for anomaly of txn: {}", description);
        
        String explanation = geminiClient.chat(SYSTEM_PROMPT, userMessage, 300);

        if (explanation != null && !explanation.isBlank()) {
            anomaly.setExplanation(explanation);
            anomalyRepository.save(anomaly);
            log.info("Successfully generated AI explanation for anomalyId={}", anomaly.getId());
        } else {
            log.warn("Gemini failed to generate explanation for anomalyId={}", anomaly.getId());
        }
    }
}
