package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.SpendingAnomaly;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpendingAnomalyRepository extends JpaRepository<SpendingAnomaly, Long> {

    /**
     * Lấy danh sách anomaly chưa bỏ qua của user trong khoảng thời gian.
     * Dùng cho API GET /api/anomalies?month=
     */
    List<SpendingAnomaly> findByUser_IdAndDetectedAtBetweenAndIsDismissedFalseOrderByDetectedAtDesc(
            Long userId, LocalDateTime from, LocalDateTime to);

    /**
     * Kiểm tra transaction đã bị gắn cờ anomaly chưa (tránh duplicate).
     */
    boolean existsByUser_IdAndTransaction_Id(Long userId, Long transactionId);

    /**
     * Lấy tất cả anomaly của 1 transaction (kể cả dismissed).
     */
    List<SpendingAnomaly> findByTransaction_Id(Long transactionId);

    /**
     * Lấy danh sách anomaly của nhóm gia đình trong khoảng thời gian.
     */
    List<SpendingAnomaly> findByTransaction_FamilyIdAndDetectedAtBetweenAndIsDismissedFalseOrderByDetectedAtDesc(
            Long groupId, LocalDateTime from, LocalDateTime to);
}
