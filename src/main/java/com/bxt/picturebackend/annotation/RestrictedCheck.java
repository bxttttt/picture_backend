package com.bxt.picturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestrictedCheck {

    /**
     * 必须不被限制 0-正常 1-被限制
     */
    int mustStatus() default 0;
}
