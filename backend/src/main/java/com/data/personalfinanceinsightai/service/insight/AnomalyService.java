package com.data.personalfinanceinsightai.service.insight;

import com.data.personalfinanceinsightai.dto.response.insight.AnomalyResponse;
import com.data.personalfinanceinsightai.entity.SpendingAnomaly;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.SpendingAnomalyRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * T12 — AnomalyService: Quản lý danh sách các chi tiêu bất thường.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyService {

    private final SpendingAnomalyRepository anomalyRepository;

    /**
     * Lấy danh sách các bất thường chưa xử lý của user trong tháng.
     */
    @Transactional(readOnly = true)
    public List<AnomalyResponse> getAnomalies(Long userId, String monthStr) {
        YearMonth ym = YearMonth.parse(monthStr);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);

        return anomalyRepository
                .findByUser_IdAndDetectedAtBetweenAndIsDismissedFalseOrderByDetectedAtDesc(userId, from, to)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách các bất thường của nhóm gia đình trong tháng.
     */
    @Transactional(readOnly = true)
    public List<AnomalyResponse> getGroupAnomalies(Long groupId, String monthStr) {
        YearMonth ym = YearMonth.parse(monthStr);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);

        return anomalyRepository
                .findByTransaction_FamilyIdAndDetectedAtBetweenAndIsDismissedFalseOrderByDetectedAtDesc(groupId, from, to)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Đánh dấu user đã đọc/bỏ qua cảnh báo bất thường.
     */
    @Transactional
    public void dismissAnomaly(Long userId, Long anomalyId) {
        SpendingAnomaly anomaly = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new ResourceNotFoundException("Anomaly not found"));
        
        if (!anomaly.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to dismiss this anomaly");
        }

        anomaly.setIsDismissed(true);
        anomalyRepository.save(anomaly);
        log.info("Anomaly {} dismissed by userId {}", anomalyId, userId);
    }

    private AnomalyResponse mapToResponse(SpendingAnomaly entity) {
        return AnomalyResponse.builder()
                .id(entity.getId())
                .transactionId(entity.getTransaction().getId())
                .description(entity.getTransaction().getDescription())
                .amount(entity.getTransaction().getAmount())
                .anomalyType(entity.getAnomalyType().name())
                .severity(entity.getSeverity().name())
                .explanation(entity.getExplanation())
                .detectedAt(entity.getDetectedAt())
                .isDismissed(entity.getIsDismissed())
                .build();
    }
}
