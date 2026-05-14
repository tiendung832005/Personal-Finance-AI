package com.data.personalfinanceinsightai.dto.request.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetCreateRequest {

    @NotNull
    private Long categoryId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "month must be yyyy-MM")
    private String month;
}
