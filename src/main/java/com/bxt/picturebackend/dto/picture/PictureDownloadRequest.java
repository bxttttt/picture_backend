package com.bxt.picturebackend.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureDownloadRequest implements Serializable {

    private static final long serialVersionUID = 5001149157504468004L;
    String picUrl;
    Long pictureId;
}
