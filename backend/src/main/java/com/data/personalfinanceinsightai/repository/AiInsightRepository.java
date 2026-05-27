package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.AiInsight;
import com.data.personalfinanceinsightai.entity.enums.InsightType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AiInsightRepository extends JpaRepository<AiInsight, Long> {

    /** Tìm insight đã cache cho user, tháng cụ thể và loại insight */
    Optional<AiInsight> findByUser_IdAndMonthAndInsightType(
            Long userId, LocalDate month, InsightType insightType);

    /** Tìm insight đã cache cho nhóm gia đình */
    Optional<AiInsight> findByGroupIdAndMonthAndInsightType(
            Long groupId, LocalDate month, InsightType insightType);

    /** Tìm tất cả insight của user theo tháng */
    List<AiInsight> findByUser_IdAndMonthOrderByCreatedAtDesc(Long userId, LocalDate month);

    /** Xóa cache cũ khi force regenerate */
    @Modifying
    @Transactional
    @Query("DELETE FROM AiInsight a WHERE a.user.id = :userId AND a.month = :month AND a.insightType = :insightType")
    void deleteByUser_IdAndMonthAndInsightType(
            @Param("userId") Long userId,
            @Param("month") LocalDate month,
            @Param("insightType") InsightType insightType);

    /** Xóa cache cũ cho nhóm gia đình */
    @Modifying
    @Transactional
    @Query("DELETE FROM AiInsight a WHERE a.groupId = :groupId AND a.month = :month AND a.insightType = :insightType")
    void deleteByGroupIdAndMonthAndInsightType(
            @Param("groupId") Long groupId,
            @Param("month") LocalDate month,
            @Param("insightType") InsightType insightType);
}
