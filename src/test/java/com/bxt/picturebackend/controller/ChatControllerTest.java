package com.bxt.picturebackend.controller;

import com.bxt.picturebackend.dto.chat.ChatRequest;
import com.bxt.picturebackend.dto.chat.ChatResponse;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.exception.GlobalExceptionHandler;
import com.bxt.picturebackend.service.AgentChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 智能客服 Chat 接口测试
 */
@WebMvcTest(ChatController.class)
@Import(GlobalExceptionHandler.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentChatService agentChatService;

    @Test
    void chat_returnsReply() throws Exception {
        ChatResponse response = new ChatResponse();
        response.setReply("你好，我是智能客服，有什么可以帮您？");
        response.setPictureIds(null);

        when(agentChatService.chat(eq("你好"), isNull(), any()))
                .thenReturn(response);

        ChatRequest request = new ChatRequest();
        request.setMessage("你好");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("你好，我是智能客服，有什么可以帮您？"));
    }

    @Test
    void chat_withSessionId_callsServiceWithSessionId() throws Exception {
        ChatResponse response = new ChatResponse();
        response.setReply("已为您找到 3 张风景图。");
        response.setPictureIds(java.util.List.of(1L, 2L, 3L));

        when(agentChatService.chat(eq("找风景图"), eq("session-123"), any()))
                .thenReturn(response);

        ChatRequest request = new ChatRequest();
        request.setMessage("找风景图");
        request.setSessionId("session-123");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("已为您找到 3 张风景图。"))
                .andExpect(jsonPath("$.data.pictureIds").isArray())
                .andExpect(jsonPath("$.data.pictureIds.length()").value(3));
    }

    @Test
    void chat_emptyMessage_returnsErrorInBody() throws Exception {
        when(agentChatService.chat(eq(""), isNull(), any()))
                .thenThrow(new BusinessException(ErrorCode.PARAMS_ERROR, "消息不能为空"));

        ChatRequest request = new ChatRequest();
        request.setMessage("");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000)); // PARAMS_ERROR
    }
}

