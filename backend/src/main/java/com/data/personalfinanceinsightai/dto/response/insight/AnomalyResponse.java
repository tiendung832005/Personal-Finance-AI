package com.data.personalfinanceinsightai.dto.response.insight;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO cho API GET /api/anomalies?month=
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnomalyResponse {

    private Long id;

    /** ID giao dịch bị gắn cờ */
    private Long transactionId;

    /** Tên/mô tả giao dịch để hiển thị */
    private String description;

    /** Số tiền giao dịch */
    private BigDecimal amount;

    /** UNUSUAL_AMOUNT / NEW_CATEGORY / FREQUENCY_SPIKE / LARGE_SINGLE_TXN */
    private String anomalyType;

    /** LOW / MEDIUM / HIGH */
    private String severity;

    /**
     * Giải thích tiếng Việt do Gemini sinh ra.
     * Có thể NULL nếu Gemini chưa xử lý xong (async).
     */
    private String explanation;

    /** Thời điểm phát hiện */
    private LocalDateTime detectedAt;

    /** User đã bỏ qua chưa */
    private Boolean isDismissed;
}
