package com.bxt.picturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bxt.picturebackend.common.PageRequest;
import com.bxt.picturebackend.dto.user.UserUpdateRequest;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.vo.UserLoginVo;
import com.bxt.picturebackend.vo.UserSearchVo;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author bxt
* @description 针对表【user】的数据库操作Service
* @createDate 2025-07-18 10:19:26
*/
public interface UserService extends IService<User> {

    long registerUser(String userAccount, String password, String confirmPassword);

    UserLoginVo loginUser(String userAccount, String password, HttpServletRequest request);

    UserLoginVo getCurrentUser(HttpServletRequest request);

    User getCompleteLoginUser(HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    /*
        用户权限校验
        权限校验其实是一个比较通用的业务需求，一般会通过Spring AOP切面+自定义权限校验注解实现统一的接口拦截和权限校验。
        如果有特殊的权限校验逻辑，再单独在接口中编码。
         */
    UserLoginVo updateUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request);

    Long getUserIdByAccount(String userAccount);

    Page<UserSearchVo> recommendUsersThroughUsername(String queryName, PageRequest pageRequest, HttpServletRequest request);
}
