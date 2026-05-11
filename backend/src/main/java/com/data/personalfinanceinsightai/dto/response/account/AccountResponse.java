package com.data.personalfinanceinsightai.dto.response.account;

import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.enums.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {

    private Long id;
    private Long userId;
    private Long familyId;
    private String name;
    private AccountType type;
    private BigDecimal balance;
    private String currency;
    private Boolean defaultAccount;
    private LocalDateTime createdAt;

    public static AccountResponse fromEntity(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .familyId(account.getFamilyId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .defaultAccount(account.getDefaultAccount())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
