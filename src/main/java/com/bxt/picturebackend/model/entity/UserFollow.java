package com.bxt.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName user_follow
 */
@TableName(value ="user_follow")
@Data
public class UserFollow {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发起关注的用户ID
     */
    private Long userId;

    /**
     * 被关注的用户ID
     */
    private Long followUserId;

    /**
     * 关注时间
     */
    private Date createTime;

    /**
     * 逻辑删除（0-未删除，1-取消关注）
     */
    private Integer isDeleted;
}