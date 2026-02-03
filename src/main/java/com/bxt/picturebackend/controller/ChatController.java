package com.bxt.picturebackend.controller;

import com.bxt.picturebackend.common.BaseResponse;
import com.bxt.picturebackend.common.ResultUtils;
import com.bxt.picturebackend.dto.chat.ChatRequest;
import com.bxt.picturebackend.dto.chat.ChatResponse;
import com.bxt.picturebackend.service.AgentChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 智能客服 Agent 对话接口（方案 C：工具调用）
 */
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @Autowired
    private AgentChatService agentChatService;

    /**
     * 发送一条消息，获取智能客服回复（支持多轮：传 sessionId）
     *
     * @param chatRequest message 必填，sessionId 选填
     * @param request     当前请求（用于获取登录用户）
     * @return reply 客服回复文本；pictureIds 若涉及搜图则返回图片 ID 列表
     */
    @PostMapping
    public BaseResponse<ChatResponse> chat(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        String message = chatRequest.getMessage();
        String sessionId = chatRequest.getSessionId();
        ChatResponse response = agentChatService.chat(message, sessionId, request);
        return ResultUtils.success(response);
    }
}
