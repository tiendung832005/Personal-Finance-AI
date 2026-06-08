package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.response.goal.GoalResponse;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoalProgressCalculator {

    private final AccountRepository accountRepository;
    private final Clock clock;

    public BigDecimal calculateCurrentAmount(FinancialGoal goal) {
        if (goal.getLinkedAccountId() == null) {
            return null;
        }

        return accountRepository.findById(goal.getLinkedAccountId())
                .filter(account -> account.getDeletedAt() == null)
                .map(Account::getBalance)
                .orElse(null);
    }

    public BigDecimal calculateMonthlyNeeded(FinancialGoal goal, BigDecimal currentAmount) {
        BigDecimal current = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        BigDecimal remaining = goal.getTargetAmount().subtract(current);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        long monthsLeft = calculateMonthsRemaining(goal);
        if (monthsLeft <= 0) {
            return remaining;
        }

        return remaining.divide(BigDecimal.valueOf(monthsLeft), 0, RoundingMode.CEILING);
    }

    public double calculateProgress(BigDecimal currentAmount, BigDecimal targetAmount) {
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        if (currentAmount.compareTo(targetAmount) >= 0) {
            return 100.0;
        }

        return currentAmount
                .divide(targetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public GoalResponse buildGoalResponse(FinancialGoal goal) {
        BigDecimal current = calculateCurrentAmount(goal);
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal monthlyNeeded = calculateMonthlyNeeded(goal, current);
        double progress = calculateProgress(current, goal.getTargetAmount());
        long monthsRemaining = calculateMonthsRemaining(goal);
        BigDecimal remaining = goal.getTargetAmount().subtract(safeCurrent).max(BigDecimal.ZERO);

        Account linkedAccount = null;
        if (goal.getLinkedAccountId() != null) {
            linkedAccount = accountRepository.findById(goal.getLinkedAccountId()).orElse(null);
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(goal.getTargetAmount())
                .deadline(goal.getDeadline())
                .status(goal.getStatus())
                .linkedAccountId(goal.getLinkedAccountId())
                .linkedAccountName(linkedAccount == null ? null : linkedAccount.getName())
                .currentAmount(current)
                .progressPercentage(progress)
                .remainingAmount(remaining)
                .monthsRemaining(monthsRemaining)
                .monthlyNeeded(monthlyNeeded)
                .onTrack(isOnTrack(goal, current))
                .createdAt(goal.getCreatedAt())
                .build();
    }

    public long calculateMonthsRemaining(FinancialGoal goal) {
        YearMonth now = YearMonth.now(clock);
        YearMonth deadlineMonth = YearMonth.from(goal.getDeadline());
        return Math.max(0, ChronoUnit.MONTHS.between(now, deadlineMonth));
    }

    private boolean isOnTrack(FinancialGoal goal, BigDecimal currentAmount) {
        if (currentAmount == null) {
            return false;
        }
        if (currentAmount.compareTo(goal.getTargetAmount()) >= 0) {
            return true;
        }

        LocalDate createdDate = goal.getCreatedAt() == null
                ? LocalDate.now(clock)
                : goal.getCreatedAt().toLocalDate();
        YearMonth createdMonth = YearMonth.from(createdDate);
        YearMonth nowMonth = YearMonth.now(clock);
        YearMonth deadlineMonth = YearMonth.from(goal.getDeadline());

        long totalMonths = Math.max(1, ChronoUnit.MONTHS.between(createdMonth, deadlineMonth));
        long elapsedMonths = ChronoUnit.MONTHS.between(createdMonth, nowMonth);
        elapsedMonths = Math.max(0, Math.min(elapsedMonths, totalMonths));

        BigDecimal expectedAmount = goal.getTargetAmount()
                .multiply(BigDecimal.valueOf(elapsedMonths))
                .divide(BigDecimal.valueOf(totalMonths), 0, RoundingMode.HALF_UP);
        return currentAmount.compareTo(expectedAmount) >= 0;
    }
}
