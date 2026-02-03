package com.bxt.picturebackend.dto.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 智能客服对话请求
 */
@Data
public class ChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户当前输入的消息
     */
    private String message;

    /**
     * 会话 ID，用于多轮对话（可选，不传则单轮）
     */
    private String sessionId;
}
