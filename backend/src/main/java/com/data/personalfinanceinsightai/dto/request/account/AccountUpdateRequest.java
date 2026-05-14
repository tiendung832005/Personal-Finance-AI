package com.data.personalfinanceinsightai.dto.request.account;

import com.data.personalfinanceinsightai.entity.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountUpdateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private AccountType type;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;
}
