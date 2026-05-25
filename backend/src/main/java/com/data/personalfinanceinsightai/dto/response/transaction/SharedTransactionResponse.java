package com.data.personalfinanceinsightai.dto.response.transaction;

import com.data.personalfinanceinsightai.entity.Transaction;
import com.data.personalfinanceinsightai.entity.enums.TransactionScope;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SharedTransactionResponse {

    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private LocalDate transactionDate;
    private Long categoryId;
    private String categoryName;
    private Long accountId;
    private String accountName;
    private Long createdByUserId;
    private String createdByName;
    private TransactionScope scope;

    @JsonProperty("isAutoCategorized")
    private boolean autoCategorized;

    private LocalDateTime createdAt;

    public static SharedTransactionResponse fromEntity(Transaction transaction) {
        String categoryName =
                transaction.getCategory() == null ? null : transaction.getCategory().getName();
        Long categoryId = transaction.getCategory() == null ? null : transaction.getCategory().getId();
        return SharedTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .accountId(transaction.getAccount().getId())
                .accountName(transaction.getAccount().getName())
                .createdByUserId(transaction.getUser().getId())
                .createdByName(transaction.getUser().getFullName())
                .scope(transaction.getScope())
                .autoCategorized(Boolean.TRUE.equals(transaction.getAutoCategorized()))
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
