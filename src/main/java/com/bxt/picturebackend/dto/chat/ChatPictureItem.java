package com.bxt.picturebackend.dto.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 智能客服返回的图片简要信息（供前端展示）
 */
@Data
public class ChatPictureItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 图片 ID */
    private Long id;
    /** 图片 URL，供前端展示 */
    private String url;
    /** 图片名称，可选展示 */
    private String name;
}
