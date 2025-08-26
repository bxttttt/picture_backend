package com.bxt.picturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bxt.picturebackend.common.PageRequest;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.dto.space.SpaceQueryRequest;
import com.bxt.picturebackend.mapper.SpaceUserMapper;
import com.bxt.picturebackend.model.entity.Picture;
import com.bxt.picturebackend.model.entity.SpaceUser;
import com.bxt.picturebackend.service.PictureService;
import com.bxt.picturebackend.service.SpaceUserService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author bxt
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-08-23 17:59:27
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService {
    /**
     * 新建团队空间
     */
    @Override
    public SpaceUser createSpace(Long userId) {
        SpaceUser spaceUser = new SpaceUser();
        long spaceId = IdWorker.getId();
        spaceUser.setSpaceId(spaceId);
        spaceUser.setUserId(userId);
        spaceUser.setSpaceRole(UserConstant.spaceRoleCreator);
        this.save(spaceUser);
        return spaceUser;
    }


    /**
     * 分页查询空间列表
     * @param spaceQueryRequest
     * @param pageRequest
     * @return
     */
    @Override
    public Page<SpaceUser> listSpaceUsers(SpaceQueryRequest spaceQueryRequest, PageRequest pageRequest) {
        Page<SpaceUser> page = new Page<>(pageRequest.getCurrent(), pageRequest.getPageSize());
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest.getUserId() != null) {
            queryWrapper.eq("userId", spaceQueryRequest.getUserId());
        }
        if (spaceQueryRequest.getSpaceId() != null) {
            queryWrapper.eq("spaceId", spaceQueryRequest.getSpaceId());
        }
        if (spaceQueryRequest.getSpaceRole() != null) {
            queryWrapper.eq("spaceRole", spaceQueryRequest.getSpaceRole());
        }
        return this.page(page, queryWrapper);
    }
    @Autowired
    private PictureService pictureService;
    /**
     * 上传图片到空间
     */
    @Override
    public long uploadPictureToSpace(Long spaceId, Long pictureId, HttpServletRequest request) {
        Picture picture = pictureService.getById(pictureId);
        picture.setSpaceId(spaceId);
        pictureService.updateById(picture);
        return pictureId;
    }
    /**
     * 把照片移出空间
     */
    @Override
    public long removePictureFromSpace(Long pictureId, HttpServletRequest request) {
        Picture picture = pictureService.getById(pictureId);
        picture.setSpaceId(null);
        pictureService.updateById(picture);
        return pictureId;
    }



}




