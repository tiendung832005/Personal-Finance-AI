package com.data.personalfinanceinsightai.service.goal;

import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.entity.GoalMonthlySnapshot;
import com.data.personalfinanceinsightai.entity.enums.GoalStatus;
import com.data.personalfinanceinsightai.repository.FinancialGoalRepository;
import com.data.personalfinanceinsightai.repository.GoalMonthlySnapshotRepository;
import com.data.personalfinanceinsightai.service.GoalProgressCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalSnapshotService {

    private final FinancialGoalRepository goalRepository;
    private final GoalMonthlySnapshotRepository snapshotRepository;
    private final GoalProgressCalculator progressCalculator;
    private final Clock clock;

    @Transactional
    public GoalMonthlySnapshot takeMonthlySnapshot(Long goalId) {
        FinancialGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new com.data.personalfinanceinsightai.exception.ResourceNotFoundException("Goal not found"));
        YearMonth currentMonth = YearMonth.now(clock);
        return snapshotRepository.findByGoalIdAndMonth(goalId, currentMonth.atDay(1))
                .orElseGet(() -> snapshotRepository.save(buildSnapshot(goal, currentMonth)));
    }

    @Transactional
    @Scheduled(cron = "0 10 0 1 * *")
    public void takeMonthlySnapshotsForActiveGoals() {
        YearMonth currentMonth = YearMonth.now(clock);
        goalRepository.findByStatus(GoalStatus.ACTIVE).forEach(goal -> {
            if (snapshotRepository.findByGoalIdAndMonth(goal.getId(), currentMonth.atDay(1)).isEmpty()) {
                snapshotRepository.save(buildSnapshot(goal, currentMonth));
            }
        });
    }

    private GoalMonthlySnapshot buildSnapshot(FinancialGoal goal, YearMonth month) {
        BigDecimal currentAmount = progressCalculator.calculateCurrentAmount(goal);
        BigDecimal savedAmount = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        BigDecimal plannedAmount = calculatePlannedAmount(goal, month);
        boolean onTrack = currentAmount != null && savedAmount.compareTo(plannedAmount) >= 0;

        return GoalMonthlySnapshot.builder()
                .goalId(goal.getId())
                .month(month.atDay(1))
                .savedAmount(savedAmount)
                .plannedAmount(plannedAmount)
                .onTrack(onTrack)
                .build();
    }

    private BigDecimal calculatePlannedAmount(FinancialGoal goal, YearMonth month) {
        YearMonth createdMonth = goal.getCreatedAt() == null
                ? YearMonth.now(clock)
                : YearMonth.from(goal.getCreatedAt().toLocalDate());
        YearMonth deadlineMonth = YearMonth.from(goal.getDeadline());
        long totalMonths = Math.max(1, ChronoUnit.MONTHS.between(createdMonth, deadlineMonth));
        long elapsedMonths = ChronoUnit.MONTHS.between(createdMonth, month);
        elapsedMonths = Math.max(0, Math.min(elapsedMonths, totalMonths));

        return goal.getTargetAmount()
                .multiply(BigDecimal.valueOf(elapsedMonths))
                .divide(BigDecimal.valueOf(totalMonths), 0, RoundingMode.HALF_UP);
    }
}
