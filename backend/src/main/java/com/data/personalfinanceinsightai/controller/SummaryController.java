package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.summary.SummaryResponse;
import com.data.personalfinanceinsightai.dto.response.summary.TrendResponse;
import com.data.personalfinanceinsightai.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String month) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(summaryService.getSummary(principal.getUsername(), month)));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<TrendResponse>> getTrend(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Integer months) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(summaryService.getTrend(principal.getUsername(), months)));
    }
}
