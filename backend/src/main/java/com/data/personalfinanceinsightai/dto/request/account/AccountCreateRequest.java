package com.data.personalfinanceinsightai.dto.request.account;

import com.data.personalfinanceinsightai.entity.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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

    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must be zero or positive")
    @Digits(integer = 13, fraction = 2, message = "Balance must have at most 13 integer digits and 2 decimals")
    private BigDecimal balance;

    @Size(min = 3, max = 3)
    private String currency;

    private Long familyId;

    private Boolean defaultAccount;
}
