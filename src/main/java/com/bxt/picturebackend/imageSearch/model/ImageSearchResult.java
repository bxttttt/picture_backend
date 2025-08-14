package com.bxt.picturebackend.imageSearch.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ImageSearchResult implements Serializable {

    private static final long serialVersionUID = 8828952369745317676L;

    // 图片url
//    private String fromUrl;
    private String thumbUrl;
}
