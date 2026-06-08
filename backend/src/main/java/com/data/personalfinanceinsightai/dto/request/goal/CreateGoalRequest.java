package com.data.personalfinanceinsightai.dto.request.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGoalRequest {

    @NotBlank(message = "Goal name is required")
    @Size(max = 200)
    private String name;

    @NotNull
    @DecimalMin(value = "1000.0", message = "targetAmount must be at least 1000")
    @Digits(integer = 13, fraction = 2, message = "targetAmount must have at most 13 integer digits and 2 decimals")
    private BigDecimal targetAmount;

    @NotNull
    @Future(message = "deadline must be a future date")
    private LocalDate deadline;

    private Long linkedAccountId;
}
