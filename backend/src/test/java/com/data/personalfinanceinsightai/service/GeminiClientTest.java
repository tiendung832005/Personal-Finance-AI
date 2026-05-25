package com.data.personalfinanceinsightai.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GeminiClientTest {

    @Autowired
    private GeminiClient geminiClient;

    @Test
    void testConnection() {
        String result = geminiClient.chat(
                "Bạn là assistant phân loại giao dịch tài chính.",
                "Phân loại giao dịch: 'Grab đi làm'"
        );
        System.out.println("Gemini response: " + result);
        assertNotNull(result, "Kết nối Gemini thất bại — kiểm tra API key");
    }
}
