package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.insight.InsightResponse;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import com.data.personalfinanceinsightai.service.insight.InsightService;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * T09 — InsightController: Cung cấp API tương tác với AI Insights.
 */
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;
    private final GroupAuthorizationService groupAuthService;
    private final UserRepository userRepository;

    /**
     * GET /api/insights/monthly?month=2026-05
     * Lấy nhận xét chi tiêu hàng tháng từ AI (có cache).
     */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<InsightResponse>> getMonthlyInsight(
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
        InsightResponse response = insightService.getMonthlyInsight(userId, targetMonth);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/insights/monthly/regenerate?month=2026-05
     * Yêu cầu AI tính toán và tạo lại nhận xét mới (xóa cache cũ).
     */
    @PostMapping("/monthly/regenerate")
    public ResponseEntity<ApiResponse<InsightResponse>> regenerate(
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
        InsightResponse response = insightService.regenerateInsight(userId, targetMonth);
        
        return ResponseEntity.ok(ApiResponse.success("Đã làm mới nhận xét từ AI", response));
    }

    /**
     * GET /api/insights/family/{groupId}?month=2026-05
     * Lấy nhận xét chi tiêu cho nhóm gia đình.
     */
    @GetMapping("/family/{groupId}")
    public ResponseEntity<ApiResponse<InsightResponse>> getFamilyInsight(
            @AuthenticationPrincipal UserDetails principal,
            @org.springframework.web.bind.annotation.PathVariable Long groupId,
            @RequestParam(required = false) String month) {
            
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }

        // Kiểm tra quyền: Phải là thành viên trong nhóm mới được xem
        String targetMonth = (month == null || month.isBlank()) 
                ? YearMonth.now().toString() 
                : month;

        Long userId = insightService.getUserIdByEmail(principal.getUsername());
        
        // Sửa lỗi: Gọi đúng phương thức requireMember của GroupAuthorizationService
        groupAuthService.requireMember(groupId, userId);

        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        InsightResponse response = insightService.getFamilyInsight(groupId, targetMonth, user);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    /**
     * POST /api/insights/family/{groupId}/regenerate?month=2026-05
     * Yêu cầu AI tạo lại nhận xét cho nhóm gia đình.
     */
    @PostMapping("/family/{groupId}/regenerate")
    public ResponseEntity<ApiResponse<InsightResponse>> regenerateFamilyInsight(
            @AuthenticationPrincipal UserDetails principal,
            @org.springframework.web.bind.annotation.PathVariable Long groupId,
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

        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        InsightResponse response = insightService.regenerateFamilyInsight(groupId, targetMonth, user);
        
        return ResponseEntity.ok(ApiResponse.success("Đã làm mới nhận xét gia đình từ AI", response));
    }
}
