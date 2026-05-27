package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.ChatCompletionRequest;
import com.data.personalfinanceinsightai.entity.User;

public interface AiService {
    String testGeminiConnection();
    String askGemini(String question);
    String chatWithGemini(User user, ChatCompletionRequest request);
}


