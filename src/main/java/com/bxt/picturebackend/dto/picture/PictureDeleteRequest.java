package com.bxt.picturebackend.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureDeleteRequest implements Serializable {

    private static final long serialVersionUID = 5113871144775139991L;

    private Long id;
}
