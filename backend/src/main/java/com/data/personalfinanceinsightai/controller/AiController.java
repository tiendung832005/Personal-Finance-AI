package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Test Gemini connection.
     * GET http://localhost:8080/api/ai/test
     */
    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        String result = aiService.testGeminiConnection();
        return ResponseEntity.ok(result);
    }

    /**
     * Ask Gemini a question.
     * GET http://localhost:8080/api/ai/ask?question=Hello
     */
    @GetMapping("/ask")
    public ResponseEntity<String> ask(@RequestParam String question) {
        String result = aiService.askGemini(question);
        return ResponseEntity.ok(result);
    }
}

