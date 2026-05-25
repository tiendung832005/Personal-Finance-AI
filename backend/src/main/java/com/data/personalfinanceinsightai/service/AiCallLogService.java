package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.entity.AiCallLog;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.repository.AiCallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCallLogService {

    private final AiCallLogRepository logRepository;

    public void logSuccess(User user, String feature, String model,
                           int promptTokens, int completionTokens, int latencyMs) {
        logRepository.save(AiCallLog.builder()
                .user(user)
                .feature(feature)
                .model(model)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .latencyMs(latencyMs)
                .success(true)
                .build());
    }

    public void logFailure(User user, String feature, String errorMessage) {
        logRepository.save(AiCallLog.builder()
                .user(user)
                .feature(feature)
                .success(false)
                .errorMessage(errorMessage)
                .latencyMs(0)
                .build());
    }
}
