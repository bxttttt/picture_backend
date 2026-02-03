package com.bxt.picturebackend.service;

import com.bxt.picturebackend.dto.chat.ChatResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 智能客服 Agent 对话服务（方案 C：工具调用）
 */
public interface AgentChatService {

    /**
     * 发送一条用户消息，执行 Agent（含工具调用），返回客服回复及可选图片 ID 列表
     *
     * @param message   用户输入
     * @param sessionId 会话 ID（可选，用于多轮；不传则单轮）
     * @param request   当前请求（用于获取登录用户，供 get_my_recent_uploads 等使用）
     * @return 回复内容 + 若涉及搜图则带 pictureIds
     */
    ChatResponse chat(String message, String sessionId, HttpServletRequest request);
}
