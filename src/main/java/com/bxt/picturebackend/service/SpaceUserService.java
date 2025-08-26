package com.bxt.picturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bxt.picturebackend.common.PageRequest;
import com.bxt.picturebackend.dto.space.SpaceQueryRequest;
import com.bxt.picturebackend.model.entity.SpaceUser;
import jakarta.servlet.http.HttpServletRequest;


/**
* @author bxt
* @description 针对表【spaceUser(空间用户关联)】的数据库操作Service
* @createDate 2025-08-23 17:59:27
*/
public interface SpaceUserService extends IService<SpaceUser> {


    SpaceUser createSpace(Long userId);

    Page<SpaceUser> listSpaceUsers(SpaceQueryRequest spaceQueryRequest, PageRequest pageRequest);

    long uploadPictureToSpace(Long spaceId, Long pictureId, HttpServletRequest request);

    long removePictureFromSpace(Long pictureId, HttpServletRequest request);
}
