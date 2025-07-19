package com.bxt.picturebackend.aop;

import com.bxt.picturebackend.annotation.AuthCheck;
import com.bxt.picturebackend.annotation.RestrictedCheck;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.model.enums.UserRoleEnum;
import com.bxt.picturebackend.model.enums.UserStatusEnum;
import com.bxt.picturebackend.service.UserService;
import com.bxt.picturebackend.vo.UserLoginVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RestrictedInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param restrictedCheck 权限校验注解
     */
    @Around("@annotation(restrictedCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, RestrictedCheck restrictedCheck) throws Throwable {
        int mustStatus = restrictedCheck.mustStatus();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User loginUser = userService.getCompleteLoginUser(request);
        UserStatusEnum mustStatusEnum = UserStatusEnum.getEnumByValue(mustStatus);
        // 不需要权限，放行
        if (mustStatusEnum == null) {
            return joinPoint.proceed();
        }
        // 以下为：必须有该权限才通过
        // 获取当前用户具有的权限
        UserStatusEnum userStatusEnum = UserStatusEnum.getEnumByValue(loginUser.getUserStatus());
        // 没有权限，拒绝
        if (userStatusEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (UserStatusEnum.NORMAL.equals(mustStatusEnum) && !UserStatusEnum.NORMAL.equals(userStatusEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
