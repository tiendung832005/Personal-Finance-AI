package com.data.personalfinanceinsightai.dto.response.budget;

import com.data.personalfinanceinsightai.entity.Budget;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BudgetResponse {

    private final Long id;
    private final Long categoryId;
    private final String categoryName;
    private final BigDecimal amount;
    private final String month;

    public static BudgetResponse fromEntity(Budget budget) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .amount(budget.getAmount())
                .month(budget.getMonth())
                .build();
    }
}
