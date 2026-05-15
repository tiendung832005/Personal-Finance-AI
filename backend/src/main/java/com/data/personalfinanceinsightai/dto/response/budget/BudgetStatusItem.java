package com.data.personalfinanceinsightai.dto.response.budget;

import com.data.personalfinanceinsightai.entity.enums.BudgetLineStatus;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BudgetStatusItem {

    private final Long categoryId;
    private final String categoryName;
    private final BigDecimal budgetAmount;
    private final BigDecimal actualAmount;
    private final BigDecimal remainingAmount;
    private final double usedPercentage;
    private final BudgetLineStatus status;
}
