package com.data.personalfinanceinsightai.service.goal;

import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.entity.enums.GoalStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoalAlertService {

    private final Clock clock;

    public GoalAlertStatus checkAlert(FinancialGoal goal, BigDecimal currentAmount, double actualProgress) {
        if (goal.getStatus() == GoalStatus.PAUSED || currentAmount == null) {
            return GoalAlertStatus.NONE;
        }
        if (actualProgress >= 100.0 || goal.getStatus() == GoalStatus.COMPLETED) {
            return GoalAlertStatus.COMPLETED;
        }
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            return GoalAlertStatus.NONE;
        }

        YearMonth createdMonth = goal.getCreatedAt() == null
                ? YearMonth.now(clock)
                : YearMonth.from(goal.getCreatedAt().toLocalDate());
        YearMonth currentMonth = YearMonth.now(clock);
        YearMonth deadlineMonth = YearMonth.from(goal.getDeadline());
        long totalMonths = ChronoUnit.MONTHS.between(createdMonth, deadlineMonth);
        if (totalMonths <= 0) {
            return GoalAlertStatus.NONE;
        }

        long elapsedMonths = ChronoUnit.MONTHS.between(createdMonth, currentMonth);
        elapsedMonths = Math.max(0, Math.min(elapsedMonths, totalMonths));
        double expectedProgress = elapsedMonths * 100.0 / totalMonths;
        double gap = expectedProgress - actualProgress;

        if (gap >= 20.0) {
            return GoalAlertStatus.SIGNIFICANTLY_BEHIND;
        }
        if (gap >= 10.0) {
            return GoalAlertStatus.SLIGHTLY_BEHIND;
        }
        return GoalAlertStatus.ON_TRACK;
    }

    public String getAlertMessage(GoalAlertStatus status, FinancialGoal goal) {
        return switch (status) {
            case SIGNIFICANTLY_BEHIND -> "Muc tieu '" + goal.getName()
                    + "' dang cham dang ke so voi ke hoach.";
            case SLIGHTLY_BEHIND -> "Muc tieu '" + goal.getName()
                    + "' dang hoi cham, can de danh them trong thang nay.";
            case ON_TRACK -> "Muc tieu '" + goal.getName() + "' dang dung tien do.";
            case COMPLETED -> "Muc tieu '" + goal.getName() + "' da hoan thanh.";
            case NONE -> null;
        };
    }
}
