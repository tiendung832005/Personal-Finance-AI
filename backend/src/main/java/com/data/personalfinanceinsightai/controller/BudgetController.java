package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.request.budget.BudgetCreateRequest;
import com.data.personalfinanceinsightai.dto.request.budget.BudgetUpdateRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetResponse;
import com.data.personalfinanceinsightai.service.BudgetService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> list(
            @AuthenticationPrincipal UserDetails principal, @RequestParam String month) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(budgetService.listForUser(principal.getUsername(), month)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> create(
            @AuthenticationPrincipal UserDetails principal, @Valid @RequestBody BudgetCreateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(budgetService.create(principal.getUsername(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> update(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody BudgetUpdateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(budgetService.update(principal.getUsername(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        budgetService.delete(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
