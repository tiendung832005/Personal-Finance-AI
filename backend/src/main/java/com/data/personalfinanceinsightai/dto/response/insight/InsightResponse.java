package com.data.personalfinanceinsightai.dto.response.insight;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO cho API GET /api/insights/monthly
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InsightResponse {

    /** Tháng dạng "yyyy-MM", ví dụ: "2026-05" */
    private String month;

    /** Nội dung AI sinh ra bằng tiếng Việt */
    private String content;

    /** Thời điểm insight được tạo */
    private LocalDateTime createdAt;

    /**
     * true  → kết quả lấy từ cache DB (không gọi Gemini lại)
     * false → vừa generate mới từ Gemini
     */
    private Boolean isFromCache;
}
