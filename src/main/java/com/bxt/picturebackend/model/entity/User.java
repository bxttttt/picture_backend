package com.bxt.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 573587875097824200L;
    /**
     * 主键ID
     * 为了防止ID被爬虫，使用雪花算法生成ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 账号
     */

    private String userAccount;

    /**
     * 密码
     */
    private String password;

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

    /**
     * 会员开通兑换码（记录来源）
     */
    private String vipCode;

    /**
     * 编辑时间（用户主动操作）
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer isDeleted;
}