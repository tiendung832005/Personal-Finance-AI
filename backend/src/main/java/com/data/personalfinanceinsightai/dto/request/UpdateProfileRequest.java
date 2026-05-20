package com.data.personalfinanceinsightai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 20)
    private String phone;
}
