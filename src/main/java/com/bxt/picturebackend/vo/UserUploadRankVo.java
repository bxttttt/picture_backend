package com.bxt.picturebackend.vo;

import lombok.Data;

@Data
public class UserUploadRankVo {
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long uploadCount;
    private Integer rank;
}
