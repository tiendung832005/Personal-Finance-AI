package com.data.personalfinanceinsightai.service;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rate limiter cho AI endpoints.
 * Mỗi user: tối đa 30 AI calls / phút (cache hit không tính, chỉ tính khi gọi Gemini thật).
 * Dùng sliding window pattern với ConcurrentHashMap — không cần thêm dependency.
 */
@Component
@Slf4j
public class AiRateLimiter {

    /** Max AI calls per user per window */
    private static final int MAX_CALLS_PER_WINDOW = 30;

    /** Window size in milliseconds (1 minute) */
    private static final long WINDOW_MS = 60_000L;

    /** Timestamp deque per user — tracks call times within the sliding window */
    private final Map<Long, Deque<Long>> userCallTimestamps = new ConcurrentHashMap<>();

    /**
     * Kiểm tra xem user có được phép gọi AI không.
     * @return true nếu còn quota, false nếu vượt rate limit.
     */
    public boolean tryConsume(Long userId) {
        if (userId == null) {
            return true; // Không có userId → không giới hạn
        }

        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        Deque<Long> timestamps = userCallTimestamps
                .computeIfAbsent(userId, id -> new ConcurrentLinkedDeque<>());

        // Xóa các timestamp cũ ngoài window
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        // Kiểm tra quota
        if (timestamps.size() >= MAX_CALLS_PER_WINDOW) {
            log.warn("Rate limit exceeded: userId={}, calls={}/{}",
                    userId, timestamps.size(), MAX_CALLS_PER_WINDOW);
            return false;
        }

        // Ghi nhận call mới
        timestamps.addLast(now);
        return true;
    }

    /**
     * Cleanup stale entries mỗi 5 phút để tránh memory leak.
     * User không gọi AI > 5 phút → xóa khỏi map.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void cleanupStaleEntries() {
        long windowStart = System.currentTimeMillis() - WINDOW_MS;
        userCallTimestamps.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            // Xóa timestamps cũ
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            // Nếu deque trống → xóa user khỏi map
            return timestamps.isEmpty();
        });
    }
}
