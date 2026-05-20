package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.ChangePasswordRequest;
import com.data.personalfinanceinsightai.dto.request.UpdateProfileRequest;
import com.data.personalfinanceinsightai.dto.response.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getProfile(String email);

    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);

    void changePassword(String email, ChangePasswordRequest request);
}
