package com.bxt.picturebackend.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/*
为每一个接口定义一个专门的类用来接收请求参数是一个良好的实践，这样可以使代码更清晰，易于维护和扩展。
 */
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 2103871907437759467L;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 密码
     */
    private String password;
    /**
     * 确认密码
     */
    private String confirmPassword;
}
