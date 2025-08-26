package com.bxt.picturebackend.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.model.entity.SpaceUser;
import com.bxt.picturebackend.service.SpaceUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PermissionUtil {
    @Resource
    private SpaceUserService spaceUserService;

    public void checkPermission(Long spaceId, Long userId, String permission){
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId);
        queryWrapper.eq("userId", userId);
        SpaceUser spaceUser = spaceUserService.getOne(queryWrapper);
        if (spaceUser == null || !spaceUser.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限操作该空间");
        }
        // 这里可以根据 permission 进行更细粒度的权限检查
        String role = spaceUser.getSpaceRole();
        List<String> perms = new ArrayList<>();
        switch (role) {
            case UserConstant.spaceRoleCreator -> {
                perms.add(UserConstant.PERM_SPACE_ADD_MEMBER);
                perms.add(UserConstant.PERM_SPACE_EDIT_MEMBER_ROLE);
                perms.add(UserConstant.PERM_SPACE_REMOVE_MEMBER);
                perms.add(UserConstant.PERM_SPACE_DELETE);
                perms.add(UserConstant.PERM_SPACE_ADD_PICTURE);
                perms.add(UserConstant.PERM_SPACE_REMOVE_PICTURE);
                perms.add(UserConstant.PERM_SPACE_VIEW);
            }
            case UserConstant.spaceRoleAdmin -> {
                perms.add(UserConstant.PERM_SPACE_ADD_MEMBER);
                perms.add(UserConstant.PERM_SPACE_REMOVE_MEMBER);
                perms.add(UserConstant.PERM_SPACE_ADD_PICTURE);
                perms.add(UserConstant.PERM_SPACE_REMOVE_PICTURE);
                perms.add(UserConstant.PERM_SPACE_VIEW);
            }
            case UserConstant.spaceRoleEditor -> {
                perms.add(UserConstant.PERM_SPACE_ADD_PICTURE);
                perms.add(UserConstant.PERM_SPACE_REMOVE_PICTURE);
                perms.add(UserConstant.PERM_SPACE_VIEW);
            }
            case UserConstant.spaceRoleViewer -> {
                perms.add(UserConstant.PERM_SPACE_VIEW);
            }
        }
        if (!perms.contains(permission)) {
            throw new RuntimeException("没有权限执行该操作");
        }
    }
}
