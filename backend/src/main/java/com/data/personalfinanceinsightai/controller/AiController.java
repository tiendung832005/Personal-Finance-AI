package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.request.ChatCompletionRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final UserRepository userRepository;

    /**
     * Test Gemini connection.
     */
    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        String result = aiService.testGeminiConnection();
        return ResponseEntity.ok(result);
    }

    /**
     * Ask Gemini a question (simple version).
     */
    @GetMapping("/ask")
    public ResponseEntity<String> ask(@RequestParam String question) {
        String result = aiService.askGemini(question);
        return ResponseEntity.ok(result);
    }

    /**
     * Chat with Gemini with history and rate limiting.
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<String>> chat(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ChatCompletionRequest request) {
        
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để sử dụng chat."));
        }

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        String result = aiService.chatWithGemini(user, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}


