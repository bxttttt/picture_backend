package com.bxt.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName user_blacklist
 */
@TableName(value ="user_blacklist")
@Data
public class UserBlacklist {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发起拉黑的用户ID
     */
    private Long userId;

    /**
     * 被拉黑的用户ID
     */
    private Long blackUserId;

    /**
     * 拉黑理由（可选）
     */
    private String reason;

    /**
     * 拉黑时间
     */
    private Date createTime;

    /**
     * 逻辑删除（0-正常，1-已删除）
     */
    private Integer isDeleted;
}