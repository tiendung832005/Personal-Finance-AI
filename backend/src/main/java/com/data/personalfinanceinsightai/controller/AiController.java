package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * Test OpenAI connection
     * GET http://localhost:8080/api/ai/test
     */
    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        String result = aiService.testOpenAiConnection();
        return ResponseEntity.ok(result);
    }

    /**
     * Ask ChatGPT a question
     * GET http://localhost:8080/api/ai/ask?question=Hello
     */
    @GetMapping("/ask")
    public ResponseEntity<String> ask(@RequestParam String question) {
        String result = aiService.askChatGPT(question);
        return ResponseEntity.ok(result);
    }
}

