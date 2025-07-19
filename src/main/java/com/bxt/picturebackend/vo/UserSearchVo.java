package com.bxt.picturebackend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserSearchVo implements Serializable {

    private static final long serialVersionUID = 8330538704944647712L;
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
     * 状态（0-正常，1-被限制）
     */
    private Integer userStatus;


    /**
     * 是否会员（0-否，1-是）
     */
    private Integer isVip;

}
