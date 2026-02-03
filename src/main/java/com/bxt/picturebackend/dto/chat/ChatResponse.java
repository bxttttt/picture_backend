package com.bxt.picturebackend.dto.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 智能客服对话响应
 */
@Data
public class ChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客服回复的文本
     */
    private String reply;

    /**
     * 若本次回复涉及搜图，返回命中的图片 ID 列表（供前端展示）
     */
    private List<Long> pictureIds;
}
