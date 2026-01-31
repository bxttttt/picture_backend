package com.bxt.picturebackend.dto.picture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureFileDeleteEvent implements Serializable {

    private static final long serialVersionUID = -4447154828443170112L;
    /**
     * COS 中的原始 key（不带 webp 后缀）
     */
    private String cosKey;
}
