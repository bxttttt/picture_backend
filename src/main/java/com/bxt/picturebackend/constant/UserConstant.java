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
    // 空间角色
    String spaceRoleViewer="viewer";
    String spaceRoleEditor="editor";
    String spaceRoleAdmin="admin";
    String spaceRoleCreator="creator";
    // 权限标识
    public static final String PERM_SPACE_EDIT_MEMBER_ROLE = "space:edit_member_role";
    public static final String PERM_SPACE_ADD_MEMBER = "space:add_member";
    public static final String PERM_SPACE_REMOVE_MEMBER = "space:remove_member";
    public static final String PERM_SPACE_DELETE = "space:delete";
    public static final String PERM_SPACE_ADD_PICTURE = "space:add_picture";
    public static final String PERM_SPACE_REMOVE_PICTURE = "space:remove_picture";
    public static final String PERM_SPACE_VIEW = "space:view";
}
