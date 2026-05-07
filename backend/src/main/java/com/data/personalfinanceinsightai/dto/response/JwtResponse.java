package com.data.personalfinanceinsightai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtResponse {

    private String token;
    private String tokenType;
    private String email;
    private String fullName;
}
