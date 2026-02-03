package com.bxt.picturebackend.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 根据图片内容推荐的名称与简介（面向爱豆应援云图库：先识别爱豆，再生成与爱豆相关的 name/introduction）
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
}
