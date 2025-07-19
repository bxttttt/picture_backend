package com.bxt.picturebackend.model.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {
    NORMAL("正常", 0),
    RESTRICTED("被限制", 1);

    private final String text;
    private final Integer value;

    UserStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    public static UserStatusEnum getEnumByValue(Integer value) {
        for (UserStatusEnum role : UserStatusEnum.values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        return null; // 或抛出异常
    }

}
