package com.data.personalfinanceinsightai.dto.request.transaction;

import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmImportRequest {

    @NotNull
    private Long accountId;

    @NotEmpty
    @Valid
    private List<ConfirmImportRow> validRows;

    @Getter
    @Setter
    public static class ConfirmImportRow {
        @NotNull
        private Integer rowNumber;

        @NotNull
        private LocalDate date;

        @NotNull
        @Positive
        private BigDecimal amount;

        @NotNull
        private TransactionType type;

        @Size(max = 500)
        private String description;

        private String note;

        @NotNull
        private Long categoryId;

        private Boolean autoCategorized;
    }
}

