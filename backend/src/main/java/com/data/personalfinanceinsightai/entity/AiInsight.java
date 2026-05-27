package com.data.personalfinanceinsightai.entity;

import com.data.personalfinanceinsightai.entity.enums.InsightType;
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
 * Lưu insight AI tháng (PERSONAL hoặc FAMILY).
 * Cache 1 lần/tháng — các request sau đọc từ DB, không gọi Gemini lại.
 */
@Entity
@Table(name = "sprint7_ai_insights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User sở hữu insight; NULL khi insight thuộc về nhóm gia đình */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Group ID (family) — nullable, dùng khi insight_type = FAMILY */
    @Column(name = "group_id")
    private Long groupId;

    /** Ngày đầu tháng, ví dụ: 2026-05-01 */
    @Column(nullable = false)
    private LocalDate month;

    @Enumerated(EnumType.STRING)
    @Column(name = "insight_type", nullable = false, length = 30)
    private InsightType insightType;

    /** Nội dung AI sinh ra bằng tiếng Việt */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Mô hình Gemini đã dùng, ví dụ: gemini-2.0-flash */
    @Column(length = 50)
    private String model;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
