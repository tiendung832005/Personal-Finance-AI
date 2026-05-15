package com.data.personalfinanceinsightai.dto.response.budget;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BudgetStatusResponse {

    private final String month;
    private final BigDecimal totalBudget;
    private final BigDecimal totalActual;
    private final BigDecimal totalRemaining;
    private final List<BudgetStatusItem> items;
}
