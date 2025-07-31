package com.bxt.picturebackend.model.enums;

import lombok.Getter;

@Getter
public enum PictureReviewStatusEnum {
    /**
     * 审核状态（0-待审核，1-审核通过，2-审核不通过）
     */
    PENDING("待审核", 0),
    APPROVED("已通过", 1),
    REJECTED("已拒绝", 2);

    private final String text;
    private final Integer value;

    PictureReviewStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }
    public String getText() {
        return text;
    }
    public Integer getValue() {
        return value;
    }
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        for (PictureReviewStatusEnum status : PictureReviewStatusEnum.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null; // 或抛出异常
    }
}
