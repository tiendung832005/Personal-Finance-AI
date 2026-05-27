package com.data.personalfinanceinsightai.entity;

import com.data.personalfinanceinsightai.entity.enums.ScoreLabel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Điểm sức khỏe tài chính (0–100) của mỗi user theo tháng.
 * Unique per (user_id, month).
 */
@Entity
@Table(name = "financial_health_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialHealthScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Ngày đầu tháng, ví dụ: 2026-05-01 */
    @Column(nullable = false)
    private LocalDate month;

    /** Điểm tổng hợp 0–100 */
    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    /** Điểm tiết kiệm */
    @Column(name = "savings_score")
    private Integer savingsScore;

    /** Điểm tuân thủ budget */
    @Column(name = "budget_score")
    private Integer budgetScore;

    /** Điểm xu hướng chi tiêu so tháng trước */
    @Column(name = "spending_trend_score")
    private Integer spendingTrendScore;

    /** Điểm nợ / tín dụng */
    @Column(name = "debt_score")
    private Integer debtScore;

    /** POOR / FAIR / GOOD / EXCELLENT */
    @Enumerated(EnumType.STRING)
    @Column(name = "score_label", length = 20)
    private ScoreLabel scoreLabel;

    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(name = "savings_tips", columnDefinition = "TEXT")
    private String savingsTips;

    @CreationTimestamp
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;
}
