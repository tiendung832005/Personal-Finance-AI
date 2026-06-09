package com.data.personalfinanceinsightai.service.goal;

import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.entity.enums.GoalStatus;
import com.data.personalfinanceinsightai.repository.FinancialGoalRepository;
import com.data.personalfinanceinsightai.service.GoalProgressCalculator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalStatusService {

    private final FinancialGoalRepository goalRepository;
    private final GoalProgressCalculator progressCalculator;
    private final Clock clock;

    @Transactional
    public FinancialGoal updateGoalStatus(FinancialGoal goal) {
        if (goal.getStatus() == GoalStatus.PAUSED) {
            return goal;
        }

        BigDecimal currentAmount = progressCalculator.calculateCurrentAmount(goal);
        if (currentAmount != null && currentAmount.compareTo(goal.getTargetAmount()) >= 0) {
            if (goal.getStatus() != GoalStatus.COMPLETED) {
                goal.setStatus(GoalStatus.COMPLETED);
                goalRepository.save(goal);
                log.info("Goal completed: goalId={}, name={}", goal.getId(), goal.getName());
            }
            return goal;
        }

        if (goal.getDeadline().isBefore(LocalDate.now(clock)) && goal.getStatus() == GoalStatus.ACTIVE) {
            goal.setStatus(GoalStatus.OVERDUE);
            goalRepository.save(goal);
            log.info("Goal overdue: goalId={}, name={}", goal.getId(), goal.getName());
        }

        return goal;
    }
}
