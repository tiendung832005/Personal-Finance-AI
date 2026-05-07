package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.LoginRequest;
import com.data.personalfinanceinsightai.dto.request.RegisterRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.JwtResponse;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.exception.EmailAlreadyExistsException;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.AuthService;
import com.data.personalfinanceinsightai.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public ApiResponse<JwtResponse> register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email already exists: " + normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .active(true)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        JwtResponse payload = new JwtResponse(token, "Bearer", user.getEmail(), user.getFullName());
        return ApiResponse.success("Register successful", payload);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<JwtResponse> login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedEmail));

        String token = jwtUtil.generateToken(user.getEmail());
        JwtResponse payload = new JwtResponse(token, "Bearer", user.getEmail(), user.getFullName());
        return ApiResponse.success("Login successful", payload);
    }
}
