package com.bxt.picturebackend.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ManagerAddUserRequest implements Serializable {
    private static final long serialVersionUID = 2434444067503492457L;

    /**
     * 账号
     */
    private String userAccount;


    /**
     * 角色（user/admin）
     */
    private String userRole;


    /**
     * 是否会员（0-否，1-是）
     */
    private Integer isVip;



}
