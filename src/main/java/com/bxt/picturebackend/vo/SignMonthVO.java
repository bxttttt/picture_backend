package com.bxt.picturebackend.vo;

import lombok.Data;

import java.util.List;

@Data
public class SignMonthVO {
    /**
     * yyyy-MM
     */
    private String month;

    /**
     * 已签到的日期列表（1~31）
     */
    private List<Integer> signedDays;

    /**
     * 本月累计签到次数
     */
    private Long monthSignedCount;

    /**
     * 连续签到天数（截至今天）
     */
    private Integer continueDays;
}
