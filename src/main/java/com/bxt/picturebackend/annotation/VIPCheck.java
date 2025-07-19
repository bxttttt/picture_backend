package com.bxt.picturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VIPCheck {

    /**
     * 0-不是VIP用户 1-VIP用户
     */
    int mustVIP() default 1;
}
