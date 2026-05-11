package com.data.personalfinanceinsightai.dto.request.account;

import com.data.personalfinanceinsightai.entity.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private AccountType type;

    @Size(min = 3, max = 3)
    private String currency;

    private Long familyId;

    private Boolean defaultAccount;
}
