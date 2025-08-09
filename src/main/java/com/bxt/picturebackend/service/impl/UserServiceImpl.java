package com.bxt.picturebackend.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bxt.picturebackend.bloomFilter.UserAccountBloomFilter;
import com.bxt.picturebackend.bloomFilter.UserIdBloomFilter;
import com.bxt.picturebackend.common.PageRequest;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.dto.user.UserUpdateRequest;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.exception.ThrowUtils;
import com.bxt.picturebackend.mapper.UserMapper;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.model.enums.UserRoleEnum;
import com.bxt.picturebackend.service.UserService;

import com.bxt.picturebackend.vo.UserLoginVo;
import com.bxt.picturebackend.vo.UserSearchVo;
import com.google.common.hash.BloomFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author bxt
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-07-18 10:19:26
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private UserIdBloomFilter userIdBloomFilter;
    @Autowired
    private UserAccountBloomFilter userAccountBloomFilter;

    @Override
    public long registerUser(String userAccount, String password, String confirmPassword) {
        System.out.println("开始注册用户: " + userAccount);
        // 检查账号和密码是否符合要求
        if (userAccount == null || userAccount.isEmpty() || password == null || password.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号或密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入的密码不一致");
        }
        if (userAccount.length() < 4 || userAccount.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号长度必须在4到20位之间");
        }
        if (password.length()<5|| password.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码长度必须在5到20位之间");
        }
        // 检查账号是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号已存在");
        }
        System.out.println("开始注册用户: " + userAccount);
        // 创建新用户对象
        User user = new User();
        user.setUserAccount(userAccount);
        String encodedPassword = passwordEncoder.encode(password);
        user.setPassword(encodedPassword); // 注意：实际应用中应对密码进行加密处理
        user.setUserName("momo"); // 默认用户名为账号
        user.setEditTime(DateUtil.date());
        user.setUserRole(UserRoleEnum.USER.getValue());

        // 保存用户到数据库
        boolean isSaved = this.save(user);
        if (!isSaved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
        }
        userIdBloomFilter.add(user.getId());
        userAccountBloomFilter.add(user.getUserAccount());
        return user.getId(); // 返回新用户的ID

    }
    @Override
    public UserLoginVo loginUser(String userAccount, String password, HttpServletRequest request) {
        // 检查账号和密码是否符合要求
        if (!userAccountBloomFilter.mightContain(userAccount)) throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在");
        if (userAccount == null || userAccount.isEmpty() || password == null || password.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号或密码不能为空");
        }
        if (userAccount.length() < 4 || userAccount.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号长度必须在4到20位之间");
        }
        if (password.length()<5|| password.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码长度必须在5到20位之间");
        }
        // 检查账号是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号不存在");
        }
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
        }
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user); // 将用户信息存入会话
        request.getSession().setMaxInactiveInterval(60 * 60*30);
        // 返回用户登录信息
        UserLoginVo userLoginVo = new UserLoginVo();
        BeanUtils.copyProperties(user, userLoginVo);
        return userLoginVo;
    }

    @Override
    public UserLoginVo getCurrentUser(HttpServletRequest request){
        User user = getCompleteLoginUser(request);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或会话已过期");
        }
        UserLoginVo userLoginVo = new UserLoginVo();
        BeanUtils.copyProperties(user, userLoginVo);
        return userLoginVo;
    }

    @Override
    public User getCompleteLoginUser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        Long userId = user != null ? user.getId() : null;
        if (!userIdBloomFilter.mightContain(userId)) return null;
        user= this.getById(userId);
        System.out.println(user);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或会话已过期");
        }
        return user; // 返回当前登录的用户信息
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object user=request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或会话已过期");
        }
        // 清除会话中的用户信息
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true; // 返回成功状态
    }
    /*
    用户权限校验
    权限校验其实是一个比较通用的业务需求，一般会通过Spring AOP切面+自定义权限校验注解实现统一的接口拦截和权限校验。
    如果有特殊的权限校验逻辑，再单独在接口中编码。
     */
    @Override
    public UserLoginVo updateUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request) {

        if (userUpdateRequest==null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        User user = getCompleteLoginUser(request);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或会话已过期");
        }
        Date todayStart = DateUtil.beginOfDay(new Date());
        if (user.getEditTime() == null||user.getIsVip()==UserConstant.USER_VIP_YES||!user.getEditTime().after(todayStart)) {
            //更新操作
            BeanUtils.copyProperties(userUpdateRequest, user);
            user.setEditTime(new Date());
//            System.out.println("用户信息更新: " + userUpdateRequest);
//            System.out.println("更新用户信息: " + user);
            boolean isUpdated = this.updateById(user);
            ThrowUtils.throwIf(!isUpdated, ErrorCode.SYSTEM_ERROR, "更新用户信息失败");
        }
        else if (user.getEditTime().after(todayStart)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "今天已经修改过了，明天再来吧");
        }
        UserLoginVo userLoginVo=new UserLoginVo();
        BeanUtils.copyProperties(user, userLoginVo);
        userAccountBloomFilter.add(userLoginVo.getUserAccount());
        return userLoginVo; // 返回更新后的用户信息

    }
    @Override
    public Long getUserIdByAccount(String userAccount) {
        if (userAccount == null || userAccount.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能为空");
        }
        if (!userAccountBloomFilter.mightContain(userAccount)) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"用户不存在");
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return user.getId();
    }
    @Override
    public Page<UserSearchVo> recommendUsersThroughUsername(String queryName, PageRequest pageRequest, HttpServletRequest request){
        if (queryName == null || queryName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询用户名不能为空");
        }
        if (request==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Page<User> page = new Page<>(pageRequest.getCurrent(), pageRequest.getPageSize());
        // 默认分页参数
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.like("userName",queryName);
        // 执行分页查询
        Page<User> pagedResult= this.page(page, queryWrapper);
        System.out.println("查询结果: " + pagedResult.getRecords());
        // 将 User 转换为 UserSearchVo
        Page<UserSearchVo> userSearchVoPage = new Page<>();
        userSearchVoPage.setCurrent(pagedResult.getCurrent());
        userSearchVoPage.setSize(pagedResult.getSize());
        userSearchVoPage.setTotal(pagedResult.getTotal());
        userSearchVoPage.setRecords(pagedResult.getRecords().stream().map(user -> {
            UserSearchVo userSearchVo = new UserSearchVo();
            BeanUtils.copyProperties(user, userSearchVo);
            return userSearchVo;
        }).toList());
        // 返回结果
        return userSearchVoPage;
    }
}




