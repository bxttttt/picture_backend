package com.bxt.picturebackend.constant;

import io.swagger.v3.oas.models.security.SecurityScheme;

public interface UserConstant {
    String USER_LOGIN_STATE = "userLoginState"; // 用户登录状态的key
    String CAPTCHA_ID = "captchaId"; // 验证码ID的key
    String ROLE_USER="user";
    String ROLE_ADMIN = "admin"; // 管理员角色
    int USER_STATUS_NORMAL = 0; // 用户状态正常
    int USER_STATUS_RESTRICTED = 1; // 用户状态被限制
    int USER_VIP_NO = 0; // 非会员状态
    int USER_VIP_YES = 1; // 会员状态
    String USER_DEFAULT_PASSWORD="12345678";
    int NOT_VIP_MAX_DOWNLOAD_TIMES=3;
}
