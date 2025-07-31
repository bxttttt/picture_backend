package com.bxt.picturebackend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {
    private static final long serialVersionUID = -4652535969863151929L;
    private List<String> tags;
    private List<String> categories;
}
