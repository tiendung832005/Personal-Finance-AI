package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.FinancialHealthScore;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialHealthScoreRepository extends JpaRepository<FinancialHealthScore, Long> {

    /** Lấy điểm sức khỏe tài chính của user trong tháng cụ thể */
    Optional<FinancialHealthScore> findByUser_IdAndMonth(Long userId, LocalDate month);

    /** Lịch sử điểm của user (cho biểu đồ xu hướng) */
    List<FinancialHealthScore> findByUser_IdAndMonthBetweenOrderByMonthAsc(
            Long userId, LocalDate from, LocalDate to);

    /** Kiểm tra đã tính điểm tháng này chưa */
    boolean existsByUser_IdAndMonth(Long userId, LocalDate month);
}
