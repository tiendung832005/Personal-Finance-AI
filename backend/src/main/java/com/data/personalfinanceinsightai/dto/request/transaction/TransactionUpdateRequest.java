package com.data.personalfinanceinsightai.dto.request.transaction;

import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionUpdateRequest {

    private Long accountId;

    private Long categoryId;

    private Long familyId;

    @Positive
    private BigDecimal amount;

    private TransactionType type;

    @Size(max = 500)
    private String description;

    private LocalDate transactionDate;

    @Size(max = 5000)
    private String note;
}
