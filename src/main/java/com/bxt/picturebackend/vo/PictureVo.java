package com.bxt.picturebackend.vo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.bxt.picturebackend.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PictureVo implements Serializable {
    private static final long serialVersionUID = -5222884265982278426L;

    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    private UserLoginVo userLoginVo;

    /**
     * 缩略图 URL
     */
    private String thumbnailUrl;

    public static Picture voToObj(PictureVo pictureVo){
        if (pictureVo == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVo, picture);
        picture.setTags(pictureVo.getTags() == null ? null : JSONUtil.toJsonStr(pictureVo.getTags()));
        return picture;
    }

    public static PictureVo objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVo pictureVo = new PictureVo();
        BeanUtils.copyProperties(picture, pictureVo);
        String tagsJson = picture.getTags();
        if (StrUtil.isNotBlank(tagsJson) && tagsJson.trim().startsWith("[")) {
            pictureVo.setTags(JSONUtil.toList(tagsJson, String.class));
        } else {
            pictureVo.setTags(null); // 或 Collections.emptyList() 看你业务需求
        }
        return pictureVo;
    }

}
