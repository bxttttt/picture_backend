package com.bxt.picturebackend.service;

import com.bxt.picturebackend.aliYunAi.ChatCompletionResponse;
import com.bxt.picturebackend.aliYunAi.DashScopeClient;
import com.bxt.picturebackend.dto.chat.ChatResponse;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.service.impl.AgentChatServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 智能客服 AgentChatServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AgentChatServiceImplTest {

    @Mock
    private DashScopeClient dashScopeClient;

    @Mock
    private PictureService pictureService;

    @Mock
    private UserService userService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AgentChatServiceImpl agentChatService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void chat_blankMessage_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
                agentChatService.chat("", null, request));
        assertThrows(BusinessException.class, () ->
                agentChatService.chat("   ", null, request));
        verifyNoInteractions(dashScopeClient);
    }

    @Test
    void chat_directReplyWithoutToolCalls_returnsReply() throws Exception {
        ChatCompletionResponse.Message message = new ChatCompletionResponse.Message();
        message.setRole("assistant");
        message.setContent("你好，有什么可以帮您？");
        message.setToolCalls(null);

        when(dashScopeClient.chatWithTools(anyList(), anyList())).thenReturn(message);

        ChatResponse response = agentChatService.chat("你好", null, request);

        assertNotNull(response);
        assertEquals("你好，有什么可以帮您？", response.getReply());
        assertNull(response.getPictureIds());
        verify(dashScopeClient, times(1)).chatWithTools(anyList(), anyList());
    }

    @Test
    void chat_withSessionId_loadsAndSavesSession() throws Exception {
        when(valueOperations.get(anyString())).thenReturn(null);

        ChatCompletionResponse.Message message = new ChatCompletionResponse.Message();
        message.setRole("assistant");
        message.setContent("收到。");
        message.setToolCalls(null);

        when(dashScopeClient.chatWithTools(anyList(), anyList())).thenReturn(message);

        ChatResponse response = agentChatService.chat("测试", "session-abc", request);

        assertNotNull(response);
        assertEquals("收到。", response.getReply());
        verify(valueOperations).get(startsWith("chat:session:"));
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
    }
}
