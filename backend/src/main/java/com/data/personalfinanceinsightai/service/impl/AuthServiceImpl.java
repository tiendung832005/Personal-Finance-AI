package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.ForgotPasswordResetRequest;
import com.data.personalfinanceinsightai.dto.request.GoogleAuthRequest;
import com.data.personalfinanceinsightai.dto.request.LoginRequest;
import com.data.personalfinanceinsightai.dto.request.OtpRequest;
import com.data.personalfinanceinsightai.dto.request.RegisterRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.JwtResponse;
import com.data.personalfinanceinsightai.entity.OtpVerification;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.OtpPurpose;
import com.data.personalfinanceinsightai.exception.EmailAlreadyExistsException;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.OtpVerificationRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.AuthService;
import com.data.personalfinanceinsightai.util.JwtUtil;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;
    private final WebClient.Builder webClientBuilder;

    private static final int OTP_EXPIRE_MINUTES = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Value("${app.mail.from:no-reply@pfia.local}")
    private String mailFrom;

    @Value("${app.mail.fallback-log-otp:true}")
    private boolean fallbackLogOtp;

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

    @Override
    @Transactional
    public ApiResponse<Void> requestGoogleOtp(OtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email already exists: " + normalizedEmail);
        }
        sendOtp(normalizedEmail, OtpPurpose.REGISTER);
        return ApiResponse.success("OTP sent to email", null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> requestForgotPasswordOtp(OtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (!userRepository.existsByEmail(normalizedEmail)) {
            throw new ResourceNotFoundException("User not found");
        }
        sendOtp(normalizedEmail, OtpPurpose.FORGOT_PASSWORD);
        return ApiResponse.success("OTP sent to email", null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetPasswordWithOtp(ForgotPasswordResetRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        verifyOtpOrThrow(normalizedEmail, request.getOtp(), OtpPurpose.FORGOT_PASSWORD);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ApiResponse.success("Password reset successful", null);
    }

    @Override
    @Transactional
    public ApiResponse<JwtResponse> authWithGoogle(GoogleAuthRequest request) {
        GoogleTokenInfo tokenInfo = verifyGoogleIdToken(request.getIdToken());
        String normalizedEmail = tokenInfo.email().trim().toLowerCase();
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Google account does not provide a valid email");
        }

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            String otp = request.getOtp() == null ? "" : request.getOtp().trim();
            if (otp.isBlank()) {
                throw new IllegalArgumentException("OTP is required for Google sign up");
            }
            verifyOtpOrThrow(normalizedEmail, otp, OtpPurpose.REGISTER);
            user = userRepository.save(User.builder()
                    .email(normalizedEmail)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(tokenInfo.name() == null || tokenInfo.name().isBlank() ? normalizedEmail : tokenInfo.name().trim())
                    .avatarUrl(tokenInfo.picture())
                    .active(true)
                    .build());
        }

        String token = jwtUtil.generateToken(user.getEmail());
        JwtResponse payload = new JwtResponse(token, "Bearer", user.getEmail(), user.getFullName());
        return ApiResponse.success("Google authentication successful", payload);
    }

    private void sendOtp(String email, OtpPurpose purpose) {
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        OtpVerification verification = OtpVerification.builder()
                .email(email)
                .purpose(purpose)
                .otpCode(otp)
                .verified(false)
                .expiresAt(LocalDateTime.now(com.data.personalfinanceinsightai.config.AppTimeConfig.APP_ZONE).plusMinutes(OTP_EXPIRE_MINUTES))
                .build();
        otpVerificationRepository.save(verification);
        sendOtpMail(email, otp, purpose);
    }

    private void verifyOtpOrThrow(String email, String otp, OtpPurpose purpose) {
        OtpVerification verification = otpVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("OTP is invalid or expired"));

        if (Boolean.TRUE.equals(verification.getVerified())
                || verification.getExpiresAt().isBefore(LocalDateTime.now(com.data.personalfinanceinsightai.config.AppTimeConfig.APP_ZONE))
                || !verification.getOtpCode().equals(otp)) {
            throw new IllegalArgumentException("OTP is invalid or expired");
        }

        verification.setVerified(true);
        otpVerificationRepository.save(verification);
    }

    private void sendOtpMail(String email, String otp, OtpPurpose purpose) {
        String action = purpose == OtpPurpose.REGISTER ? "dang ky tai khoan" : "dat lai mat khau";
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailFrom);
        mail.setTo(email);
        mail.setSubject("Ma OTP PersonalFinanceInsightAI");
        mail.setText("Ma OTP de " + action + " la: " + otp + ". Ma co hieu luc trong " + OTP_EXPIRE_MINUTES + " phut.");
        try {
            mailSender.send(mail);
        } catch (MailException ex) {
            log.error("Failed to send OTP mail to {}", email, ex);
            if (fallbackLogOtp) {
                log.warn("MAIL_FALLBACK_LOG_OTP=true -> OTP for {} (purpose={}) is: {}", email, purpose, otp);
                return;
            }
            throw new IllegalArgumentException("Cannot send OTP email. Check MAIL_USERNAME, MAIL_PASSWORD (App Password), and MAIL_FROM.");
        }
    }

    private GoogleTokenInfo verifyGoogleIdToken(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("Google client id is not configured");
        }

        Map<String, Object> payload;
        try {
            payload = webClientBuilder.build()
                    .get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .map(body -> new ResponseStatusException(response.statusCode(), body)))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Google token is invalid");
        }

        if (payload == null) {
            throw new IllegalArgumentException("Google token is invalid");
        }

        String aud = asString(payload.get("aud"));
        String email = asString(payload.get("email"));
        String emailVerified = asString(payload.get("email_verified"));

        if (!googleClientId.equals(aud)) {
            throw new IllegalArgumentException("Google token audience mismatch");
        }
        if (!"true".equalsIgnoreCase(emailVerified)) {
            throw new IllegalArgumentException("Google email is not verified");
        }

        return new GoogleTokenInfo(
                email,
                asString(payload.get("name")),
                asString(payload.get("picture"))
        );
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record GoogleTokenInfo(String email, String name, String picture) {
    }
}
