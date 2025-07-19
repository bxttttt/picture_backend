package com.bxt.picturebackend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserLoginVo implements Serializable {

    private static final long serialVersionUID = 8128028663915892574L;
    private Long id;

    /**
     * 账号
     */

    private String userAccount;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 角色（user/admin）
     */
    private String userRole;

    /**
     * 状态（0-正常，1-被限制）
     */
    private Integer userStatus;

    /**
     * 限制过期时间（仅当userStatus=1时有效）
     */
    private Date restrictedExpireTime;

    /**
     * 是否会员（0-否，1-是）
     */
    private Integer isVip;

    /**
     * 会员过期时间
     */
    private Date vipExpireTime;


}
