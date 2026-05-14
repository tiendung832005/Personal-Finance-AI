package com.data.personalfinanceinsightai.dto.request.budget;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetUpdateRequest {

    @NotNull
    @Positive
    private BigDecimal amount;
}
