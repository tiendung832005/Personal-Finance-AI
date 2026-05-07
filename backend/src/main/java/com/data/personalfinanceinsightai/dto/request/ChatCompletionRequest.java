package com.data.personalfinanceinsightai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatCompletionRequest {
    private String model;
    private List<ChatMessage> messages;
    private int max_tokens;
    private double temperature;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}

