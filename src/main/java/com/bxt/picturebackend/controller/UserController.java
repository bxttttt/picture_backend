package com.bxt.picturebackend.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bxt.picturebackend.annotation.AuthCheck;
import com.bxt.picturebackend.annotation.RestrictedCheck;
import com.bxt.picturebackend.annotation.VIPCheck;
import com.bxt.picturebackend.bloomFilter.UserIdBloomFilter;
import com.bxt.picturebackend.common.BaseResponse;
import com.bxt.picturebackend.common.PageRequest;
import com.bxt.picturebackend.common.ResultUtils;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.dto.user.*;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.model.enums.UserRoleEnum;
import com.bxt.picturebackend.service.UserService;
import com.bxt.picturebackend.vo.UserLoginVo;
import com.bxt.picturebackend.vo.UserSearchVo;
import com.google.code.kaptcha.Producer;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.SslBundleSslEngineFactory;
import org.springframework.boot.autoconfigure.pulsar.PulsarAutoConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    UserIdBloomFilter userIdBloomFilter;
    /*
    用户注册接口
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getPassword();
        String confirmPassword = userRegisterRequest.getConfirmPassword();
        Long result= userService.registerUser(userAccount, password, confirmPassword);
        return ResultUtils.success(result);
    }
    /*
    登录时图形验证码接口
     */
    @Autowired
    private Producer captchaProducer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取图形验证码
     * @param response 响应
     * @return 验证码图片流
     */
    @GetMapping("/captcha")
    public BaseResponse<String> getCaptcha(HttpServletResponse response,HttpServletRequest request) throws IOException {
        // 1. 生成验证码文本
        String captchaText = captchaProducer.createText();

        // 2. 生成唯一的验证码ID（前端需要同时保存此ID）
        String captchaId = IdUtil.simpleUUID();
        request.getSession().setAttribute(UserConstant.CAPTCHA_ID, captchaId);
        // 3. 将验证码文本存入 Redis，有效期 5 分钟
        stringRedisTemplate.opsForValue().set("captcha:" + captchaId, captchaText, 5, TimeUnit.MINUTES);

        // 4. 生成验证码图片
        BufferedImage image = captchaProducer.createImage(captchaText);

        // 5. 设置响应头
        response.setContentType("image/png");
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "png", out);
        out.close();
        return ResultUtils.success(captchaId);
    }
    @PostMapping("/login")
    public BaseResponse<UserLoginVo> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String userAccount = userLoginRequest.getUserAccount();
        String password = userLoginRequest.getPassword();
        String captcha = userLoginRequest.getCaptcha();
        String captchaId = request.getSession().getAttribute(UserConstant.CAPTCHA_ID) != null ? request.getSession().getAttribute("captchaId").toString() : null;

        if (userAccount == null || userAccount.isEmpty() ||
                password == null || password.isEmpty() ||
                captcha == null || captcha.isEmpty() ||
                captchaId == null || captchaId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 从 Redis 取出对应验证码
        String redisCaptcha = stringRedisTemplate.opsForValue().get("captcha:" + captchaId);
        if (redisCaptcha == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期或不存在");
        }

        if (!redisCaptcha.equalsIgnoreCase(captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }

        // 验证通过，执行登录逻辑
        UserLoginVo userLoginVo=new UserLoginVo();
        userLoginVo = userService.loginUser(userAccount, password,request);

        // 登录成功后，删除 Redis 中验证码，避免重复使用
        stringRedisTemplate.delete("captcha:" + captchaId);

        return ResultUtils.success(userLoginVo);
    }
    @GetMapping("/getCurrentUser")
//    @VIPCheck(mustVIP = UserConstant.USER_VIP_YES)
    public BaseResponse<UserLoginVo> getCurrentUser(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        UserLoginVo userLoginVo = userService.getCurrentUser(request);
        return ResultUtils.success(userLoginVo);
    }
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }
    @PostMapping("/user/update")
    public BaseResponse<UserLoginVo> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        UserLoginVo result = userService.updateUser(userUpdateRequest, request);
        return ResultUtils.success(result);
    }
    @PostMapping("/manager/addUser")
    public BaseResponse<Long> addUser(@RequestBody ManagerAddUserRequest managerAddUserRequest){
        if (managerAddUserRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        String userAccount = managerAddUserRequest.getUserAccount();
        String password = UserConstant.USER_DEFAULT_PASSWORD;
        String confirmPassword = UserConstant.USER_DEFAULT_PASSWORD;
        Long result= userService.registerUser(userAccount, password, confirmPassword);
        return ResultUtils.success(result);
    }
    @GetMapping("/getUserIdByAccount")
    public BaseResponse<Long> getUserIdByAccount(@RequestParam String userAccount, HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或会话已过期");
        }
        if (userAccount == null || userAccount.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能为空");
        }
        Long userId = userService.getUserIdByAccount(userAccount);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return ResultUtils.success(userId);
    }
    @PostMapping("/manager/updateUser/password")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Boolean> updateUserPassword(@RequestBody ManagerUpdateUserPassword managerUpdateUserPassword, HttpServletRequest request) {
        User user = userService.getById(managerUpdateUserPassword.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (managerUpdateUserPassword.getPassword() == null || managerUpdateUserPassword.getPassword().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能为空");
        }else if (managerUpdateUserPassword.getPassword().length() < 5|| managerUpdateUserPassword.getPassword().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能小于8位");
        }
        user.setPassword(managerUpdateUserPassword.getPassword());
        boolean result=userService.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户密码失败");
        }
        return ResultUtils.success(result);
    }
    @PostMapping("/manager/updateUser/status")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Boolean> updateUserStatus(@RequestBody ManagerUpdateUserStatus managerUpdateUserStatus, HttpServletRequest request) {
        User user = userService.getById(managerUpdateUserStatus.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (managerUpdateUserStatus.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户状态不能为空");
        }
        user.setUserStatus(managerUpdateUserStatus.getStatus());
        boolean result=userService.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户状态失败");
        }
        return ResultUtils.success(result);
    }
    @PostMapping("/manager/updateUser/vip")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Boolean> updateUserVipStatus(@RequestBody ManagerUpdateUserVipStatus managerUpdateUserVipStatus, HttpServletRequest request) {
        User user = userService.getById(managerUpdateUserVipStatus.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (managerUpdateUserVipStatus.getIsVip() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员状态不能为空");
        }
        user.setIsVip(managerUpdateUserVipStatus.getIsVip());
        Date expireTime = managerUpdateUserVipStatus.getVipExpireTime();
        if (expireTime==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员过期时间不能为空");
        }
        // 如果时间比现在早
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员过期时间不能早于当前时间");
        }
        user.setVipExpireTime(expireTime);
        boolean result=userService.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户会员状态失败");
        }
        return ResultUtils.success(result);
    }
    @PostMapping("/manager/updateUser/auth")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Boolean> updateUserAuthStatus(@RequestBody ManagerUpdateUserAuthStatus managerUpdateUserAuthStatus, HttpServletRequest request) {
        User user = userService.getById(managerUpdateUserAuthStatus.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (managerUpdateUserAuthStatus == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(managerUpdateUserAuthStatus.getUserRole());
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色不合法");
        }
        user.setUserRole(userRoleEnum.getValue());
        boolean result = userService.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户角色失败");
        }
        return ResultUtils.success(result);
    }
    @GetMapping("manager/getUserById")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<User> getUserById(@RequestParam Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空或无效");
        }
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return ResultUtils.success(user);
    }
    @PostMapping("/manager/deleteUserById")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Boolean> deleteUserById(@RequestBody Long userId, HttpServletRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空或无效");
        }
        boolean result = userService.removeById(userId);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除用户失败");
        }
        return ResultUtils.success(result);
    }
    @GetMapping("/getUserById")
    public BaseResponse<UserLoginVo> getUserVOById(@RequestParam Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空或无效");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或会话已过期");
        }
        if (!userIdBloomFilter.mightContain(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        UserLoginVo userLoginVo = new UserLoginVo();
        BeanUtils.copyProperties(user, userLoginVo);
        return ResultUtils.success(userLoginVo);
    }
    @GetMapping("/searchUserByUsername")
    public BaseResponse<Page<UserSearchVo>> searchUserByUsername(String username, PageRequest pageRequest,HttpServletRequest httpServletRequest){
        if (httpServletRequest == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或会话已过期");
        }
        if (username==null||pageRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        Page<UserSearchVo> userSearchVoPage = userService.recommendUsersThroughUsername(username,pageRequest,httpServletRequest);
        return ResultUtils.success(userSearchVoPage);

    }



}
