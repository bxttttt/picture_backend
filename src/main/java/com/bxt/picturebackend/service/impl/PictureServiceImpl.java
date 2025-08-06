package com.bxt.picturebackend.service.impl;
import java.util.Date;
import java.util.List;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bxt.picturebackend.dto.file.UploadPictureResult;
import com.bxt.picturebackend.dto.picture.PictureQueryRequest;
import com.bxt.picturebackend.dto.picture.PictureReviewRequest;
import com.bxt.picturebackend.dto.picture.PictureUploadRequest;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.exception.ThrowUtils;
import com.bxt.picturebackend.manager.FileManager;
import com.bxt.picturebackend.model.entity.Picture;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.service.PictureService;
import com.bxt.picturebackend.mapper.PictureMapper;
import com.bxt.picturebackend.service.UserService;
import com.bxt.picturebackend.vo.PictureVo;
import com.bxt.picturebackend.vo.UserLoginVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
* @author bxt
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-07-21 17:11:40
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{
    @Autowired
    private FileManager fileManager;
    @Autowired
    private UserService userService;
    @Autowired
    private ProjectInfoAutoConfiguration projectInfoAutoConfiguration;

    @Override
    public PictureVo uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (loginUser==null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        Long pictureId=null;
        if (pictureUploadRequest.getId()!=null){
            pictureId = pictureUploadRequest.getId();
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            if (!exists) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
        }
        // 上传图片，得到信息
        String picturePrefix = "public/"+loginUser.getId().toString();
        UploadPictureResult uploadResult = fileManager.uploadPicture(multipartFile, picturePrefix);
        if (uploadResult == null || uploadResult.getUrl() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
        // 创建或更新图片记录
        Picture picture = new Picture();
        picture.setUrl(uploadResult.getUrl());
        picture.setName(uploadResult.getPicName());
        picture.setIntroduction("");
        picture.setCategory("");
        picture.setTags("");
        picture.setPicSize(uploadResult.getPicSize());
        picture.setPicWidth(Math.toIntExact(uploadResult.getPicWidth()));
        picture.setPicHeight(Math.toIntExact(uploadResult.getPicHeight()));
        picture.setPicScale(uploadResult.getPicScale());
        picture.setPicFormat(uploadResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setEditTime(new Date());
        if (pictureId != null) {
            picture.setId(pictureId);
            boolean updateResult = this.updateById(picture);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片更新失败");
            }
        } else {
            boolean saveResult = this.save(picture);
            System.out.println("保存图片结果: " + saveResult);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败");
            }
        }
        System.out.println("上传图片成功，图片ID: " + picture.getId());

        return PictureVo.objToVo(picture);
    }
    @Override
    public PictureVo uploadPicture(PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (loginUser==null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        Long pictureId=null;
        if (pictureUploadRequest.getId()!=null){
            pictureId = pictureUploadRequest.getId();
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            if (!exists) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
        }
        // 上传图片，得到信息
        String picturePrefix = "public/"+loginUser.getId().toString();
        System.out.println(pictureUploadRequest);
        UploadPictureResult uploadResult = fileManager.uploadPicture(pictureUploadRequest.getFileUrl(), picturePrefix);
        if (uploadResult == null || uploadResult.getUrl() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
        // 创建或更新图片记录
        Picture picture = new Picture();
        picture.setUrl(uploadResult.getUrl());
        picture.setName(uploadResult.getPicName());
        picture.setIntroduction("");
        picture.setCategory("");
        picture.setTags("");
        picture.setPicSize(uploadResult.getPicSize());
        picture.setPicWidth(Math.toIntExact(uploadResult.getPicWidth()));
        picture.setPicHeight(Math.toIntExact(uploadResult.getPicHeight()));
        picture.setPicScale(uploadResult.getPicScale());
        picture.setPicFormat(uploadResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setEditTime(new Date());
        if (pictureId != null) {
            picture.setId(pictureId);
            boolean updateResult = this.updateById(picture);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片更新失败");
            }
        } else {
            boolean saveResult = this.save(picture);
            System.out.println("保存图片结果: " + saveResult);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败");
            }
        }
        System.out.println("上传图片成功，图片ID: " + picture.getId());

        return PictureVo.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest){
        QueryWrapper<Picture> queryWrapper=new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id= pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        Long userId = pictureQueryRequest.getUserId();
        String searchText = pictureQueryRequest.getSearchText();
        if (StrUtil.isNotBlank(searchText)){
            queryWrapper.nested(qw ->
                            qw.like("introduction", searchText)
                                    .or()
                                    .like("name", searchText)
                    );

        }
        if (id != null) {
            System.out.println("查询图片ID: " + id);
            queryWrapper.eq("id", id);
        }
        if (StrUtil.isNotBlank(name)) {
            queryWrapper.like("name", name);
        }
        if (StrUtil.isNotBlank(introduction)) {
            queryWrapper.like("introduction", introduction);
        }
        if (StrUtil.isNotBlank(category)) {
            queryWrapper.eq("category", category);
        }
        if (picSize != null) {
            queryWrapper.eq("picSize", picSize);
        }
        if (picWidth != null) {
            queryWrapper.eq("picWidth", picWidth);
        }
        if (picHeight != null) {
            queryWrapper.eq("picHeight", picHeight);
        }
        if (picScale != null) {
            queryWrapper.eq("picScale", picScale);
        }
        if (StrUtil.isNotBlank(picFormat)) {
            queryWrapper.eq("picFormat", picFormat);
        }
        for (String tag : tags) {
            if (StrUtil.isNotBlank(tag)) {
                queryWrapper.like("tags", tag);
            }
        }
        return queryWrapper;
    }
    @Override
    public PictureVo getPictureVo(Picture picture, HttpServletRequest request){
        UserLoginVo userLoginVo=userService.getCurrentUser(request);
        PictureVo pictureVo = PictureVo.objToVo(picture);
        if (userLoginVo != null) {
            pictureVo.setUserLoginVo(userLoginVo);
        }
        return pictureVo;
    }
    @Override
    public PictureVo getPictureVo(Picture picture, Long userId) {
        User user= userService.getById(userId);
        UserLoginVo userLoginVo = null;
        BeanUtils.copyProperties(user, userLoginVo);
        PictureVo pictureVo = PictureVo.objToVo(picture);
        if (userLoginVo != null) {
            pictureVo.setUserLoginVo(userLoginVo);
        }
        return pictureVo;
    }
    @Override
    public Page<PictureVo> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        List<PictureVo> pictureVoList = pictureList.stream()
                .map(picture -> getPictureVo(picture, picture.getUserId()))
                .toList();
        Page<PictureVo> pictureVoPage = new Page<>();
        pictureVoPage.setRecords(pictureVoList);
        pictureVoPage.setCurrent(picturePage.getCurrent());
        pictureVoPage.setSize(picturePage.getSize());
        pictureVoPage.setTotal(picturePage.getTotal());
        pictureVoPage.setPages(picturePage.getPages());
        return pictureVoPage;
    }
    @Override
    public void validPicture(Picture picture) {
        if (picture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
        }
        Long id = picture.getId();
        String url= picture.getUrl();
        String name = picture.getName();
        String introduction = picture.getIntroduction();
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        if (StrUtil.isNotBlank(introduction)){
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "图片简介过长");
        }

    }
    @Override
    public boolean doPictureReview(PictureReviewRequest pictureReviewRequest, Long reviewerId){
        Picture picture = this.getById(pictureReviewRequest.getId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        picture.setReviewReason(pictureReviewRequest.getReviewReason());
        picture.setReviewStatus(pictureReviewRequest.getStatus());
        picture.setReviewUserId(reviewerId);
        picture.setReviewTime(new Date());
        boolean updateResult = this.updateById(picture);
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "图片审核失败");
        return updateResult;
    }



}




