package com.data.personalfinanceinsightai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequest {

    @NotBlank(message = "idToken must not be blank")
    private String idToken;

    @Pattern(regexp = "^$|\\d{6}", message = "OTP must be exactly 6 digits")
    private String otp;
}
