package com.data.personalfinanceinsightai.service;

public interface AiService {
    String testOpenAiConnection();
    String askChatGPT(String question);
}

