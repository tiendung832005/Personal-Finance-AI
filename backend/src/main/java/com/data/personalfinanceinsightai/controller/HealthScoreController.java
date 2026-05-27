package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.insight.HealthScoreResponse;
import com.data.personalfinanceinsightai.service.insight.HealthScoreService;
import com.data.personalfinanceinsightai.service.insight.InsightService;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * T14 — HealthScoreController: API lấy điểm sức khỏe tài chính.
 */
@RestController
@RequestMapping("/api/health-score")
@RequiredArgsConstructor
public class HealthScoreController {

    private final HealthScoreService healthScoreService;
    private final InsightService insightService;

    /**
     * GET /api/health-score?month=2026-05
     */
    @GetMapping
    public ResponseEntity<ApiResponse<HealthScoreResponse>> getHealthScore(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String month) {
            
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }

        String targetMonth = (month == null || month.isBlank()) 
                ? YearMonth.now().toString() 
                : month;

        Long userId = insightService.getUserIdByEmail(principal.getUsername());
        HealthScoreResponse response = healthScoreService.getHealthScore(userId, targetMonth);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/health-score/regenerate?month=2026-05
     */
    @org.springframework.web.bind.annotation.PostMapping("/regenerate")
    public ResponseEntity<ApiResponse<HealthScoreResponse>> regenerateHealthScore(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String month) {
            
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }

        String targetMonth = (month == null || month.isBlank()) 
                ? YearMonth.now().toString() 
                : month;

        Long userId = insightService.getUserIdByEmail(principal.getUsername());
        HealthScoreResponse response = healthScoreService.regenerateHealthScore(userId, targetMonth);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
