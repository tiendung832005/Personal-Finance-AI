package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.request.transaction.ConfirmImportRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.transaction.imports.ImportConfirmResponse;
import com.data.personalfinanceinsightai.dto.response.transaction.imports.ImportPreviewResponse;
import com.data.personalfinanceinsightai.service.ImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/transactions/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping
    public ResponseEntity<ApiResponse<ImportPreviewResponse>> importPreview(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(importService.previewImport(principal.getUsername(), file)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ImportConfirmResponse>> confirmImport(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ConfirmImportRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(importService.confirmImport(principal.getUsername(), request)));
    }
}


