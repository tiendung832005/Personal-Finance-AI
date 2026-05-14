package com.data.personalfinanceinsightai.dto.request.transaction;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionUpdateRequest {

    private Long categoryId;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Size(max = 500)
    private String description;

    private LocalDate transactionDate;
}
