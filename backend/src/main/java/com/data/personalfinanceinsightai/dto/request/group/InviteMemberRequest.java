package com.data.personalfinanceinsightai.dto.request.group;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteMemberRequest {
    @NotBlank
    @Email
    private String email;
}
