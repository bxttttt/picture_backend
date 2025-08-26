package com.bxt.picturebackend.dto.space;

import com.bxt.picturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceQueryRequest implements Serializable {
    private static final long serialVersionUID = -7923161215630490264L;
    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private PageRequest pageRequest;

}
