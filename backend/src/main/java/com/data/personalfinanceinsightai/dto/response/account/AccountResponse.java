package com.data.personalfinanceinsightai.dto.response.account;

import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.enums.AccountScope;
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
    private Long groupId;
    private AccountScope scope;
    private String name;
    private AccountType type;
    private BigDecimal balance;
    private BigDecimal currentBalance;
    private String currency;
    private Boolean defaultAccount;
    private LocalDateTime createdAt;

    public static AccountResponse fromEntity(Account account) {
        return fromEntityWithBalance(account, account.getBalance());
    }

    public static AccountResponse fromEntityWithBalance(Account account, BigDecimal currentBalance) {
        Long groupId = account.getFamilyId();
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .familyId(groupId)
                .groupId(groupId)
                .scope(account.getScope())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .currentBalance(currentBalance)
                .currency(account.getCurrency())
                .defaultAccount(account.getDefaultAccount())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
