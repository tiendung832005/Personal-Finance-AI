package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.LoginRequest;
import com.data.personalfinanceinsightai.dto.request.RegisterRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.JwtResponse;

public interface AuthService {

    ApiResponse<JwtResponse> register(RegisterRequest request);

    ApiResponse<JwtResponse> login(LoginRequest request);
}
