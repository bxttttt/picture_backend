package com.bxt.picturebackend.dto.picture;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SearchPictureByPictureRequest implements Serializable {
    private static final long serialVersionUID = 456942055166725645L;
    private Long pictureId;
}
