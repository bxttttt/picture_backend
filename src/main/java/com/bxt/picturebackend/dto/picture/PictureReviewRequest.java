package com.bxt.picturebackend.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureReviewRequest implements Serializable {
    private static final long serialVersionUID = 8165953208013570232L;
    /**
     * 图片 ID
     */
    private Long id;
    /**
     * 审核状态
     * 0 - 待审核
     * 1 - 审核通过
     * 2 - 审核不通过
     */
    private Integer status;
    /**
     * 审核备注
     */
    private String reviewReason;

}
