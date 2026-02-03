package com.bxt.picturebackend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 根据图片内容推荐的名称、简介与标签（面向爱豆应援云图库：爱豆名、风格、场景、情绪等）
 */
@Data
public class PictureAiSuggestVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 识别到的爱豆/明星姓名，若无或无法识别则为空或「无」 */
    private String idolName;
    /** 推荐名称（尽量包含爱豆名字或应援相关） */
    private String name;
    /** 推荐简介（尽量包含爱豆名字或应援相关） */
    private String introduction;
    /** 推荐标签列表：可包含爱豆名字、图片风格、照片场景、情绪风格等 */
    private List<String> tags = new ArrayList<>();
}
