package com.bxt.picturebackend.dto.picture;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.awt.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PictureUpdateRequest implements Serializable {
    private static final long serialVersionUID = 5133833540266183417L;
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

}
