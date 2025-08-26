package com.bxt.picturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.model.entity.SpaceUser;
import com.bxt.picturebackend.service.SpaceUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // loginId 就是 userId
        Long userId = Long.parseLong(loginId.toString());
        // 这里你需要查询数据库：获取用户在当前 space 的角色
        String spaceRole = queryUserSpaceRole(userId);

        List<String> perms = new ArrayList<>();
        switch (spaceRole) {
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
        return perms;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long spaceUserId = Long.parseLong(loginId.toString());
        String spaceRole = queryUserSpaceRole(spaceUserId);
        return List.of(spaceRole); // 直接返回用户的空间角色
    }
    @Autowired
    private SpaceUserService spaceUserService;
    // 模拟查询数据库（你需要改成真正的 service）
    private String queryUserSpaceRole(Long spaceUserId) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", spaceUserId);
        SpaceUser spaceUser = spaceUserService.getOne(queryWrapper);
        if (spaceUser != null) {
            return spaceUser.getSpaceRole();
        }
        return null; // 默认没有角色
    }
}
