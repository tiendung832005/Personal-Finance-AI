package com.data.personalfinanceinsightai.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiRateLimiterTest {

    private AiRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new AiRateLimiter();
    }

    @Test
    @DisplayName("Trong limit → cho phép gọi")
    void tryConsume_withinLimit_returnsTrue() {
        // 30 lần đầu phải cho qua
        for (int i = 0; i < 30; i++) {
            assertTrue(rateLimiter.tryConsume(1L),
                    "Call #" + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Vượt limit (31 calls) → chặn")
    void tryConsume_exceedsLimit_returnsFalse() {
        // 30 lần đầu OK
        for (int i = 0; i < 30; i++) {
            rateLimiter.tryConsume(1L);
        }
        // Lần 31 phải bị chặn
        assertFalse(rateLimiter.tryConsume(1L));
    }

    @Test
    @DisplayName("Mỗi user có bucket riêng")
    void tryConsume_differentUsers_independentLimits() {
        // User 1 dùng hết 30 calls
        for (int i = 0; i < 30; i++) {
            rateLimiter.tryConsume(1L);
        }
        assertFalse(rateLimiter.tryConsume(1L));

        // User 2 vẫn còn quota
        assertTrue(rateLimiter.tryConsume(2L));
    }

    @Test
    @DisplayName("userId null → luôn cho phép (no limit)")
    void tryConsume_nullUserId_alwaysAllowed() {
        for (int i = 0; i < 50; i++) {
            assertTrue(rateLimiter.tryConsume(null));
        }
    }

    @Test
    @DisplayName("Cleanup xóa entries cũ mà không crash")
    void cleanupStaleEntries_doesNotThrow() {
        rateLimiter.tryConsume(1L);
        rateLimiter.tryConsume(2L);
        // Cleanup should not throw
        rateLimiter.cleanupStaleEntries();
    }
}
