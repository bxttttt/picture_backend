package com.bxt.picturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bxt.picturebackend.annotation.AuthCheck;
import com.bxt.picturebackend.common.BaseResponse;
import com.bxt.picturebackend.common.ResultUtils;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.dto.picture.*;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.exception.ThrowUtils;
import com.bxt.picturebackend.model.entity.Picture;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.service.PictureService;
import com.bxt.picturebackend.service.UserService;
import com.bxt.picturebackend.vo.PictureTagCategory;
import com.bxt.picturebackend.vo.PictureVo;
import com.bxt.picturebackend.vo.UserLoginVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@RestController
@Slf4j
@RequestMapping("/picture")
public class PictureController {
    @Autowired
    private UserService userService;
    @Autowired
    private PictureService pictureService;
    @PostMapping("/upload/file")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<PictureVo> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 @RequestPart(value = "pictureUploadRequest", required = false) PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest httpServletRequest) {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        PictureVo pictureVo = pictureService.uploadPicture(multipartFile, pictureUploadRequest, userService.getById(loginUser.getId()));
        return ResultUtils.success(pictureVo);
    }
    @PostMapping("/upload/url")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<PictureVo> uploadPicture(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest httpServletRequest) {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        System.out.println(pictureUploadRequest);
        PictureVo pictureVo = pictureService.uploadPicture(pictureUploadRequest, userService.getById(loginUser.getId()));
        return ResultUtils.success(pictureVo);
    }
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody PictureDeleteRequest pictureDeleteRequest, HttpServletRequest httpServletRequest) {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        Long pictureId = pictureDeleteRequest.getId();
        Long userId=pictureService.getById(pictureId).getUserId();
        Long loginUserId = loginUser.getId();
        if (!Objects.equals(loginUser.getUserRole(), UserConstant.ROLE_ADMIN) && !userId.equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除他人图片");
        }
        boolean result = pictureService.removeById(pictureId);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片删除失败");
        }
        return ResultUtils.success(result);
    }
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest httpServletRequest) {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        Long pictureId = pictureUpdateRequest.getId();
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        if (!Objects.equals(loginUser.getUserRole(), UserConstant.ROLE_ADMIN) && !Objects.equals(picture.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改他人图片");
        }
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        picture.setEditTime(new Date());
        pictureService.validPicture(picture);
        boolean result = pictureService.updateById(picture);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片更新失败");
        }
        return ResultUtils.success(result);
    }
    @PostMapping("/getPictureById")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Picture> getPictureById(Long id, HttpServletRequest httpServletRequest) {
        Picture picture = pictureService.getById(id);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        return ResultUtils.success(picture);
    }
    @PostMapping("/getPictureById/vo")
    public BaseResponse<PictureVo> getPictureVoById(Long id, HttpServletRequest httpServletRequest) {
        Picture picture = pictureService.getById(id);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        PictureVo pictureVo = pictureService.getPictureVo(picture,picture.getUserId());
        if (pictureVo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片信息转换失败");
        }
        return ResultUtils.success(pictureVo);
    }
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVo>> listPictureVoByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR,"分页大小不能超过20");
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePage, httpServletRequest);
        return ResultUtils.success(pictureVoPage);
    }
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTags(tagList);
        pictureTagCategory.setCategories(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }
    @PostMapping("/reviewPicture")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest httpServletRequest) {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        if (!Objects.equals(loginUser.getUserRole(), UserConstant.ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限审核图片");
        }
        boolean result = pictureService.doPictureReview(pictureReviewRequest, loginUser.getId());
        return ResultUtils.success(result);
    }
}
