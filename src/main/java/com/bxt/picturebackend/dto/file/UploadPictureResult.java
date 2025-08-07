package com.bxt.picturebackend.dto.file;

import lombok.Data;

@Data
public class UploadPictureResult {
    private String url;
    private String thumbnailUrl;
    private String picName;
    private Long picSize;
    private Long picWidth;
    private Long picHeight;
    private Double picScale;
    private String picFormat;

}
