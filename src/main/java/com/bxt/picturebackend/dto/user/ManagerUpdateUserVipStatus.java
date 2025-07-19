package com.bxt.picturebackend.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ManagerUpdateUserVipStatus implements Serializable {


    private static final long serialVersionUID = -541821075152023732L;

    Long id;
    /**
     * 是否会员（0-否，1-是）
     */
    Integer isVip;

    Date vipExpireTime; // 会员到期时间，如果是0则表示没有到期时间，永久会员
}
