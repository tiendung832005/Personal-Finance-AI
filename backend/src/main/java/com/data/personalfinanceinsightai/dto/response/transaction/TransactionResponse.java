package com.data.personalfinanceinsightai.dto.response.transaction;

import com.data.personalfinanceinsightai.entity.Transaction;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionResponse {

    private Long id;
    private Long userId;
    private Long accountId;
    private Long categoryId;
    private String categoryName;
    private Long familyId;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private LocalDate transactionDate;

    @JsonProperty("isAutoCategorized")
    private boolean autoCategorized;

    @JsonProperty("isAnomaly")
    private boolean anomaly;

    private String note;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TransactionResponse fromEntity(Transaction transaction) {
        String categoryName = transaction.getCategory() == null ? null : transaction.getCategory().getName();
        Long categoryId = transaction.getCategory() == null ? null : transaction.getCategory().getId();
        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUser().getId())
                .accountId(transaction.getAccount().getId())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .familyId(transaction.getFamilyId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .autoCategorized(Boolean.TRUE.equals(transaction.getAutoCategorized()))
                .anomaly(Boolean.TRUE.equals(transaction.getFlaggedAnomaly()))
                .note(transaction.getNote())
                .deletedAt(transaction.getDeletedAt())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
