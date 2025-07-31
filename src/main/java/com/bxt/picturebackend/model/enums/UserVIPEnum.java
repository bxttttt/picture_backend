package com.bxt.picturebackend.model.enums;

import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;

@Getter
public enum UserVIPEnum {
    NORMAL("普通用户", 0),
    VIP("VIP用户", 1);

    private final String text;
    private final Integer value;

    UserVIPEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public Integer getValue() {
        return value;
    }

    public static UserVIPEnum getEnumByValue(Integer value) {
        for (UserVIPEnum role : UserVIPEnum.values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        return null; // 或抛出异常
    }
}
