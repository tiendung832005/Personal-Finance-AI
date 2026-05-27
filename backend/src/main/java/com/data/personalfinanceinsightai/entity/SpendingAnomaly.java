package com.data.personalfinanceinsightai.entity;

import com.data.personalfinanceinsightai.entity.enums.AnomalyType;
import com.data.personalfinanceinsightai.entity.enums.Severity;
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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Chi tiêu bất thường được phát hiện bởi rule-based AnomalyDetector.
 * Explanation được điền sau bởi AnomalyExplainer (Gemini) chạy async.
 */
@Entity
@Table(name = "spending_anomalies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendingAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Giao dịch bị gắn cờ bất thường */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false, length = 50)
    private AnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    /**
     * Giải thích bằng tiếng Việt do Gemini sinh ra (async).
     * Có thể NULL khi vừa được detect, sẽ được điền sau.
     */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    /** User đã đọc / bỏ qua anomaly này */
    @Builder.Default
    @Column(name = "is_dismissed", nullable = false)
    private Boolean isDismissed = false;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;
}
