package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.request.goal.CreateGoalRequest;
import com.data.personalfinanceinsightai.dto.request.goal.UpdateGoalRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.goal.GoalDetailResponse;
import com.data.personalfinanceinsightai.dto.response.goal.GoalPlanResponse;
import com.data.personalfinanceinsightai.dto.response.goal.GoalProgressResponse;
import com.data.personalfinanceinsightai.dto.response.goal.GoalResponse;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GoalService;
import com.data.personalfinanceinsightai.service.goal.GoalPlanService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final GoalPlanService goalPlanService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateGoalRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(goalService.create(principal.getUsername(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponse>>> list(
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(goalService.listForUser(principal.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalDetailResponse>> getById(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(goalService.getById(principal.getUsername(), id)));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<GoalProgressResponse>> getProgress(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(goalService.getProgress(principal.getUsername(), id)));
    }

    @GetMapping("/{id}/plan")
    public ResponseEntity<ApiResponse<GoalPlanResponse>> getPlan(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
        User user = findUser(principal);
        return ResponseEntity.ok(ApiResponse.success(goalPlanService.getOrGeneratePlan(id, user.getId())));
    }

    @PostMapping("/{id}/plan/regenerate")
    public ResponseEntity<ApiResponse<GoalPlanResponse>> regeneratePlan(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
        User user = findUser(principal);
        return ResponseEntity.ok(ApiResponse.success(goalPlanService.regeneratePlan(id, user.getId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> update(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateGoalRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(goalService.update(principal.getUsername(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        goalService.delete(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    private User findUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
