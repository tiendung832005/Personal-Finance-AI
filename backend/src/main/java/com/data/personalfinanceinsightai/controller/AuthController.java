package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.request.ForgotPasswordResetRequest;
import com.data.personalfinanceinsightai.dto.request.GoogleAuthRequest;
import com.data.personalfinanceinsightai.dto.request.LoginRequest;
import com.data.personalfinanceinsightai.dto.request.OtpRequest;
import com.data.personalfinanceinsightai.dto.request.RegisterRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.JwtResponse;
import com.data.personalfinanceinsightai.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/google/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestGoogleOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.requestGoogleOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestForgotPasswordOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.requestForgotPasswordOtp(request));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponse<Void>> resetPasswordWithOtp(@Valid @RequestBody ForgotPasswordResetRequest request) {
        return ResponseEntity.ok(authService.resetPasswordWithOtp(request));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<JwtResponse>> authWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.authWithGoogle(request));
    }
}
