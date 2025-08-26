package com.bxt.picturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bxt.picturebackend.common.BaseResponse;
import com.bxt.picturebackend.common.PageRequest;
import com.bxt.picturebackend.common.PermissionUtil;
import com.bxt.picturebackend.common.ResultUtils;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.dto.space.SpaceQueryRequest;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.model.entity.Picture;
import com.bxt.picturebackend.model.entity.SpaceUser;
import com.bxt.picturebackend.service.PictureService;
import com.bxt.picturebackend.service.SpaceUserService;
import com.bxt.picturebackend.service.UserService;
import com.bxt.picturebackend.vo.PictureVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {
    @Autowired
    private SpaceUserService spaceUserService;
    @Autowired
    private UserService userService;
    @Autowired
    private PermissionUtil permissionUtil;

    /**
     * 创建空间
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/create")
    public BaseResponse<SpaceUser> createSpace(HttpServletRequest httpServletRequest) {
        long userId = userService.getCurrentUser(httpServletRequest).getId();

        SpaceUser result=spaceUserService.createSpace(userId);
        if (result==null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"创建空间失败");
        }
        return ResultUtils.success(result);
    }
    /**
     * 查询空间列表
     * @param spaceQueryRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/query")
    public BaseResponse<Page<SpaceUser>> querySpace(@RequestBody SpaceQueryRequest spaceQueryRequest ,HttpServletRequest httpServletRequest) {
        permissionUtil.checkPermission(spaceQueryRequest.getSpaceId(),userService.getCurrentUser(httpServletRequest).getId(), UserConstant.PERM_SPACE_VIEW);
        PageRequest pageRequest = new PageRequest();
        if (spaceQueryRequest.getPageRequest() != null) {
            pageRequest = spaceQueryRequest.getPageRequest();
        }
        Page<SpaceUser> result=spaceUserService.listSpaceUsers(spaceQueryRequest,pageRequest);
        if (result==null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"查询空间失败");
        }
        return ResultUtils.success(result);
    }

    /**
     * 添加成员到空间
     * @param spaceId
     * @param userId
     */
    @PostMapping("/addMember")
    public BaseResponse<Long> addMember(@RequestParam Long spaceId, @RequestParam Long userId, @RequestParam String spaceRole,HttpServletRequest httpServletRequest) {
        permissionUtil.checkPermission(spaceId,userService.getCurrentUser(httpServletRequest).getId(), UserConstant.PERM_SPACE_ADD_MEMBER);
        // 检查用户是否存在
        if (userService.getById(userId) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        // 检查用户是否已在空间中
        SpaceUser existingSpaceUser = spaceUserService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpaceUser>()
                .eq("spaceId", spaceId)
                .eq("userId", userId));
        if (existingSpaceUser != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已在空间中");
        }
        // 添加成员，默认角色为 viewer
        SpaceUser newSpaceUser = new SpaceUser();
        newSpaceUser.setSpaceId(spaceId);
        newSpaceUser.setUserId(userId);
        if (spaceRole != null && (spaceRole.equals(UserConstant.spaceRoleViewer)
                || spaceRole.equals(UserConstant.spaceRoleEditor)
                || spaceRole.equals(UserConstant.spaceRoleAdmin))) {
            newSpaceUser.setSpaceRole(spaceRole);
        } else {
            newSpaceUser.setSpaceRole(UserConstant.spaceRoleViewer);
        }
        boolean saveResult = spaceUserService.save(newSpaceUser);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加成员失败");
        }
        return ResultUtils.success(newSpaceUser.getUserId());
    }
    /**
     * 从空间移除成员
     * @param spaceId
     * @param userId
     */
    @PostMapping("/removeMember")
    public BaseResponse<Long> removeMember(@RequestParam Long spaceId, @RequestParam Long userId,HttpServletRequest httpServletRequest) {
        permissionUtil.checkPermission(spaceId,userService.getCurrentUser(httpServletRequest).getId(), UserConstant.PERM_SPACE_REMOVE_MEMBER);
        long currentUserId = userService.getCurrentUser(httpServletRequest).getId();
        // 检查用户是否在空间中
        SpaceUser existingSpaceUser = spaceUserService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpaceUser>()
                .eq("spaceId", spaceId)
                .eq("userId", userId));
        if (existingSpaceUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不在空间中");
        }
        // 不能移除自己
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能移除自己");
        }
        boolean removeResult = spaceUserService.remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpaceUser>()
                .eq("spaceId", spaceId)
                .eq("userId", userId));
        if (!removeResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "移除成员失败");
        }
        return ResultUtils.success(userId);
    }
    /**
     * 修改成员角色
     * @param spaceId
     * @param userId
     * @param spaceRole
     */
    @PostMapping("/updateMemberRole")
    public BaseResponse<Long> updateMemberRole(@RequestParam Long spaceId, @RequestParam Long userId, @RequestParam String spaceRole,HttpServletRequest httpServletRequest) {
        permissionUtil.checkPermission(spaceId,userService.getCurrentUser(httpServletRequest).getId(), UserConstant.PERM_SPACE_EDIT_MEMBER_ROLE);
        long currentUserId = userService.getCurrentUser(httpServletRequest).getId();
        // 检查用户是否在空间中
        SpaceUser existingSpaceUser = spaceUserService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpaceUser>()
                .eq("spaceId", spaceId)
                .eq("userId", userId));
        if (existingSpaceUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不在空间中");
        }
        // 不能修改自己角色
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能修改自己角色");
        }
        // 不能将创建者降级
        if (existingSpaceUser.getSpaceRole().equals(UserConstant.spaceRoleCreator)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能修改创建者角色");
        }
        // 只能设置为 viewer/editor/admin
        if (!spaceRole.equals(UserConstant.spaceRoleViewer)
                && !spaceRole.equals(UserConstant.spaceRoleEditor)
                && !spaceRole.equals(UserConstant.spaceRoleAdmin)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色只能是 viewer/editor/admin");
        }
        existingSpaceUser.setSpaceRole(spaceRole);
        boolean updateResult = spaceUserService.updateById(existingSpaceUser);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改成员角色失败");
        }
        return ResultUtils.success(userId);
    }
    /**
     * 退出空间
     */
    @PostMapping("/leave")
    public BaseResponse<Long> leaveSpace(@RequestParam Long spaceId,HttpServletRequest httpServletRequest) {
        long currentUserId = userService.getCurrentUser(httpServletRequest).getId();
        SpaceUser existingSpaceUser = spaceUserService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpaceUser>()
                .eq("spaceId", spaceId)
                .eq("userId", currentUserId));
        if (existingSpaceUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不在空间中");
        }
        // 创建者不能退出空间
        if (existingSpaceUser.getSpaceRole().equals(UserConstant.spaceRoleCreator)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建者不能退出空间");
        }
        boolean removeResult = spaceUserService.remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpaceUser>()
                .eq("spaceId", spaceId)
                .eq("userId", currentUserId));
        if (!removeResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "退出空间失败");
        }
        return ResultUtils.success(currentUserId);
    }

    /**
     * 查询用户的所有空间
     */
    @GetMapping("/listUserSpaces")
    public BaseResponse<Page<SpaceUser>> listUserSpaces(HttpServletRequest httpServletRequest, PageRequest pageRequest) {
        permissionUtil.checkPermission(0L,userService.getCurrentUser(httpServletRequest).getId(), UserConstant.PERM_SPACE_VIEW);
        long currentUserId = userService.getCurrentUser(httpServletRequest).getId();
        SpaceQueryRequest spaceQueryRequest = new SpaceQueryRequest();
        spaceQueryRequest.setUserId(currentUserId);
        Page<SpaceUser> result = spaceUserService.listSpaceUsers(spaceQueryRequest, pageRequest);
        if (result == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询空间失败");
        }
        return ResultUtils.success(result);
    }
    /**
     * 删除空间
     */
    @PostMapping("/delete")
    public BaseResponse<Long> deleteSpace(@RequestParam Long spaceId,HttpServletRequest httpServletRequest) {
        permissionUtil.checkPermission(spaceId,userService.getCurrentUser(httpServletRequest).getId(), UserConstant.PERM_SPACE_DELETE);
        // 删除空间及其所有关联
        boolean removeResult = spaceUserService.remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpaceUser>()
                .eq("spaceId", spaceId));
        if (!removeResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除空间失败");
        }
        return ResultUtils.success(spaceId);
    }
    /*
    把照片加入空间
     */
    @PostMapping("/addPictureToSpace")
    public BaseResponse<Long> addPictureToSpace(@RequestParam Long spaceId, @RequestParam Long pictureId, HttpServletRequest httpServletRequest) {
        long currentUserId = userService.getCurrentUser(httpServletRequest).getId();
        permissionUtil.checkPermission(spaceId,currentUserId, UserConstant.PERM_SPACE_ADD_PICTURE);
        long result = spaceUserService.uploadPictureToSpace(spaceId, pictureId, httpServletRequest);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加图片到空间失败");
        }
        return ResultUtils.success(result);
    }
    /*
    从空间移除照片
     */
    @PostMapping("/removePictureFromSpace")
    public BaseResponse<Long> removePictureFromSpace(@RequestParam Long spaceId, @RequestParam Long pictureId, HttpServletRequest httpServletRequest) {
        long currentUserId = userService.getCurrentUser(httpServletRequest).getId();
        permissionUtil.checkPermission(spaceId,currentUserId, UserConstant.PERM_SPACE_REMOVE_PICTURE);
        long result = spaceUserService.removePictureFromSpace( pictureId, httpServletRequest);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "从空间移除图片失败");
        }
        return ResultUtils.success(result);
    }
    @Autowired
    private PictureService pictureService;
    /**
     * 查看空间中的所有图片
     */
    @GetMapping("/listSpacePictures")
    public BaseResponse<Page<PictureVo>> listSpacePictures(@RequestParam Long spaceId, HttpServletRequest httpServletRequest, PageRequest pageRequest) {
        permissionUtil.checkPermission(spaceId,userService.getCurrentUser(httpServletRequest).getId(), UserConstant.PERM_SPACE_VIEW);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId);
        Page<Picture> picturePage = pictureService.page(new Page<>(pageRequest.getCurrent(), pageRequest.getPageSize()), queryWrapper);
        // 转换为 PictureVo
        Page<PictureVo> pictureVoPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        List<PictureVo> pictureVoList = picturePage.getRecords().stream()
                .map(PictureVo::objToVo)
                .collect(Collectors.toList());
        pictureVoPage.setRecords(pictureVoList);

        return ResultUtils.success(pictureVoPage);
    }







}
