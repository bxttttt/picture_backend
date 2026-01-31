package com.bxt.picturebackend.constant;

/**
 * Redis Key 相关常量（前缀或完整 key）
 */
public interface RedisKeyConstant {

    /** 验证码：key = CAPTCHA_PREFIX + captchaId */
    String CAPTCHA_PREFIX = "captcha:";

    /** 图片详情缓存：key = PICTURE_ID_PREFIX + hashKey */
    String PICTURE_ID_PREFIX = "picture:id:";

    /** 图片分页/查询缓存：key = PICTURE_QUERY_PREFIX + hashKey */
    String PICTURE_QUERY_PREFIX = "picture:query:";

    /** 上传防重：key = UPLOAD_FILE_PREFIX + fileMd + userIdMd */
    String UPLOAD_FILE_PREFIX = "upload:file:";

    /** 用户上传图片数量排行榜（ZSet） */
    String PICTURE_UPLOAD_RANK = "picture:upload:rank";
}