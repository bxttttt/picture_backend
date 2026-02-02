package com.bxt.picturebackend.vo;

import lombok.Data;

@Data
public class SignInVO {
    /**
     * 今天是否签到成功（或已签到）
     */
    private Boolean todaySigned;

    /**
     * 本月累计签到次数
     */
    private Long monthSignedCount;

    /**
     * 连续签到天数（截至今天）
     */
    private Integer continueDays;
}
