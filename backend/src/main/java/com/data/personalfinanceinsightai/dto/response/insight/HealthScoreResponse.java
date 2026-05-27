package com.data.personalfinanceinsightai.dto.response.insight;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO cho API GET /api/health-score?month=
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthScoreResponse {

    /** Tháng dạng "yyyy-MM" */
    private String month;

    /** Điểm tổng hợp 0–100 */
    private Integer overallScore;

    /** Điểm tiết kiệm */
    private Integer savingsScore;

    /** Điểm tuân thủ budget */
    private Integer budgetScore;

    /** Điểm xu hướng chi tiêu so tháng trước */
    private Integer spendingTrendScore;

    /** Điểm nợ */
    private Integer debtScore;

    /** POOR / FAIR / GOOD / EXCELLENT */
    private String scoreLabel;

    /** Phân tích chi tiết từ AI cho điểm số này */
    private String aiAnalysis;

    /** Danh sách gợi ý tiết kiệm được cá nhân hóa bởi AI (cấu trúc JSON hoặc text) */
    private String savingsTips;

    /** Chi tiết từng thành phần điểm để hiển thị breakdown */
    private List<ScoreBreakdownItem> breakdown;

    @Getter
    @Builder
    public static class ScoreBreakdownItem {
        private String label;
        private Integer score;
        private Integer maxScore;
        private String description;
    }
}
