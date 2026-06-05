package com.data.personalfinanceinsightai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

import com.data.personalfinanceinsightai.dto.request.ChatCompletionRequest;
import com.data.personalfinanceinsightai.dto.request.ChatMessage;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.service.AiCallLogService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private AiCallLogService aiCallLogService;

    @Test
    void chatWithGemini_offTopicMessage_returnsWarningWithoutConsumingQuota() {
        AiServiceImpl aiService = new AiServiceImpl(aiCallLogService);
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(List.of(ChatMessage.builder()
                        .role("user")
                        .content("Hôm nay nên xem phim gì?")
                        .build()))
                .build();

        String response = aiService.chatWithGemini(new User(), request);

        assertEquals(AiServiceImpl.OFF_TOPIC_RESPONSE, response);
        verifyNoInteractions(aiCallLogService);
    }

    @Test
    void isFinanceRelated_acceptsVietnameseFinanceQuestions() {
        AiServiceImpl aiService = new AiServiceImpl(aiCallLogService);

        assertTrue(aiService.isFinanceRelated("Tôi nên lập ngân sách và tiết kiệm như thế nào?"));
        assertTrue(aiService.isFinanceRelated("Chi tiêu tháng này của tôi vượt 500k"));
    }

    @Test
    void isFinanceRelated_rejectsUnrelatedQuestionWithSimilarPrefix() {
        AiServiceImpl aiService = new AiServiceImpl(aiCallLogService);

        assertFalse(aiService.isFinanceRelated("Tôi học tiếng Anh thế nào cho hiệu quả?"));
    }
}
