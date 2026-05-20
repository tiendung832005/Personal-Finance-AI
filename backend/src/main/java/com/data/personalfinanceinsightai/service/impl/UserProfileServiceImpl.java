package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.ChangePasswordRequest;
import com.data.personalfinanceinsightai.dto.request.UpdateProfileRequest;
import com.data.personalfinanceinsightai.dto.response.UserProfileResponse;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.exception.BusinessException;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        return UserProfileResponse.fromEntity(findUser(email));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUser(email);
        user.setFullName(request.getFullName().trim());
        user.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        user.setPhone(trimToNull(request.getPhone()));
        return UserProfileResponse.fromEntity(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findUser(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must be different from current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUser(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
