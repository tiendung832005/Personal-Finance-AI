package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.insight.AnomalyResponse;
import com.data.personalfinanceinsightai.service.insight.AnomalyService;
import com.data.personalfinanceinsightai.service.insight.InsightService;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * T12 — AnomalyController: API quản lý cảnh báo chi tiêu bất thường.
 */
@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyService anomalyService;
    private final InsightService insightService;
    private final GroupAuthorizationService groupAuthService;

    /**
     * GET /api/anomalies/family/{groupId}?month=2026-05
     * Lấy danh sách các cảnh báo bất thường trong nhóm gia đình.
     */
    @GetMapping("/family/{groupId}")
    public ResponseEntity<ApiResponse<List<AnomalyResponse>>> getGroupAnomalies(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long groupId,
            @RequestParam(required = false) String month) {
            
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }

        String targetMonth = (month == null || month.isBlank()) 
                ? YearMonth.now().toString() 
                : month;

        Long userId = insightService.getUserIdByEmail(principal.getUsername());
        groupAuthService.requireMember(groupId, userId);

        List<AnomalyResponse> anomalies = anomalyService.getGroupAnomalies(groupId, targetMonth);
        
        return ResponseEntity.ok(ApiResponse.success(anomalies));
    }

    /**
     * GET /api/anomalies?month=2026-05
     * Lấy danh sách các cảnh báo bất thường trong tháng.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AnomalyResponse>>> getAnomalies(
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
        List<AnomalyResponse> anomalies = anomalyService.getAnomalies(userId, targetMonth);
        
        return ResponseEntity.ok(ApiResponse.success(anomalies));
    }

    /**
     * POST /api/anomalies/{id}/dismiss
     * Đánh dấu một cảnh báo là đã đọc/bỏ qua.
     */
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
            
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }

        Long userId = insightService.getUserIdByEmail(principal.getUsername());
        anomalyService.dismissAnomaly(userId, id);
        
        return ResponseEntity.ok(ApiResponse.success("Đã bỏ qua cảnh báo", null));
    }
}
