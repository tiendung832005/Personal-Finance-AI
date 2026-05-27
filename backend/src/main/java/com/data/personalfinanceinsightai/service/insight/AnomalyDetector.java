package com.data.personalfinanceinsightai.service.insight;

import com.data.personalfinanceinsightai.entity.SpendingAnomaly;
import com.data.personalfinanceinsightai.entity.Transaction;
import com.data.personalfinanceinsightai.entity.enums.AnomalyType;
import com.data.personalfinanceinsightai.entity.enums.Severity;
import com.data.personalfinanceinsightai.entity.enums.TransactionScope;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.data.personalfinanceinsightai.repository.SpendingAnomalyRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * T10 — AnomalyDetector: Phát hiện các giao dịch chi tiêu bất thường dựa trên quy tắc (Rule-based).
 * Không tốn AI quota ở bước này.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetector {

    private final TransactionRepository txnRepository;
    private final SpendingAnomalyRepository anomalyRepository;
    private final AnomalyExplainer anomalyExplainer;

    /**
     * Kiểm tra bất thường cho một giao dịch vừa được tạo.
     * Chạy bất đồng bộ (@Async) để không làm chậm quá trình lưu giao dịch của User.
     */
    @Async
    public void detectForTransaction(Transaction txn) {
        // Chỉ kiểm tra chi tiêu cá nhân
        if (txn.getType() != TransactionType.EXPENSE 
                || txn.getScope() != TransactionScope.PERSONAL) {
            return;
        }

        List<DetectionResult> results = new ArrayList<>();

        // Rule 1: Giao dịch cao bất thường (>3x trung bình category)
        detectUnusualAmount(txn).ifPresent(results::add);

        // Rule 2: Giao dịch đơn lẻ quá lớn (>30% thu nhập tháng này)
        detectLargeTransaction(txn).ifPresent(results::add);

        // Rule 3: Chi tiêu vào danh mục mới (chưa có giao dịch trong 3 tháng qua)
        detectNewCategory(txn).ifPresent(results::add);

        // Lưu và kích hoạt giải thích AI
        for (DetectionResult result : results) {
            saveAndTriggerExplain(txn, result);
        }
    }

    private Optional<DetectionResult> detectUnusualAmount(Transaction txn) {
        if (txn.getCategory() == null) return Optional.empty();

        // Lấy trung bình chi tiêu của category này từ 3 tháng trước đến nay
        String threeMonthsAgo = YearMonth.now().minusMonths(3).toString();
        BigDecimal avg = txnRepository.avgAmountByUserCategoryAfterMonth(
                txn.getUser().getId(), txn.getCategory().getId(), threeMonthsAgo);

        if (avg == null || avg.compareTo(BigDecimal.ZERO) == 0) return Optional.empty();

        // Ngưỡng phát hiện: gấp 3 lần trung bình
        BigDecimal threshold = avg.multiply(BigDecimal.valueOf(3));
        if (txn.getAmount().compareTo(threshold) > 0) {
            double ratio = txn.getAmount().divide(avg, 2, RoundingMode.HALF_UP).doubleValue();
            return Optional.of(new DetectionResult(
                    AnomalyType.UNUSUAL_AMOUNT,
                    ratio > 5 ? Severity.HIGH : Severity.MEDIUM,
                    String.format("Khoản chi này cao gấp %.1fx mức chi trung bình thông thường của mục %s", 
                            ratio, txn.getCategory().getName())
            ));
        }
        return Optional.empty();
    }

    private Optional<DetectionResult> detectLargeTransaction(Transaction txn) {
        // Lấy tổng thu nhập tháng này để so sánh
        String currentMonth = txn.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BigDecimal monthlyIncome = txnRepository.sumByUserTypeMonth(
                txn.getUser().getId(), "INCOME", currentMonth);

        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) == 0) return Optional.empty();

        BigDecimal thirtyPercent = monthlyIncome.multiply(BigDecimal.valueOf(0.3));
        if (txn.getAmount().compareTo(thirtyPercent) > 0) {
            int percent = txn.getAmount().divide(monthlyIncome, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
            return Optional.of(new DetectionResult(
                    AnomalyType.LARGE_SINGLE_TXN,
                    Severity.HIGH,
                    String.format("Giao dịch này chiếm tới %d%% thu nhập dự kiến trong tháng %s", percent, currentMonth)
            ));
        }
        return Optional.empty();
    }

    private Optional<DetectionResult> detectNewCategory(Transaction txn) {
        if (txn.getCategory() == null) return Optional.empty();

        String threeMonthsAgo = YearMonth.now().minusMonths(3).toString();
        boolean hasHistory = txnRepository.existsByUserCategoryAfterMonth(
                txn.getUser().getId(), txn.getCategory().getId(), threeMonthsAgo);

        if (!hasHistory) {
            return Optional.of(new DetectionResult(
                    AnomalyType.NEW_CATEGORY,
                    Severity.LOW,
                    String.format("Lần đầu tiên phát hiện chi tiêu vào mục %s trong 3 tháng qua", txn.getCategory().getName())
            ));
        }
        return Optional.empty();
    }

    private void saveAndTriggerExplain(Transaction txn, DetectionResult result) {
        // Tránh lưu trùng lặp anomaly cho cùng 1 txn và cùng quy tắc
        if (anomalyRepository.existsByUser_IdAndTransaction_Id(txn.getUser().getId(), txn.getId())) {
            return; 
        }

        SpendingAnomaly anomaly = SpendingAnomaly.builder()
                .user(txn.getUser())
                .transaction(txn)
                .anomalyType(result.type)
                .severity(result.severity)
                .explanation(null) // Sẽ được AI điền async sau
                .build();
        
        SpendingAnomaly saved = anomalyRepository.save(anomaly);
        log.info("Anomaly detected: txnId={}, type={}, severity={}", txn.getId(), result.type, result.severity);

        // Gọi AI giải thích bất đồng bộ
        anomalyExplainer.explainAnomaly(saved, result.technicalReason);
    }

    @Getter
    @AllArgsConstructor
    private static class DetectionResult {
        private final AnomalyType type;
        private final Severity severity;
        private final String technicalReason;
    }
}
