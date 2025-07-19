package com.bxt.picturebackend.aop;

import com.bxt.picturebackend.annotation.VIPCheck;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.model.entity.User;
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
public class VIPInterceptor {
    @Resource
    private UserService userService;
    @Around("@annotation(vipCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, VIPCheck vipCheck) throws Throwable {
        int mustVIP = vipCheck.mustVIP();
        RequestAttributes requestAttributes= RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request=((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User user= userService.getCompleteLoginUser(request);
        if (mustVIP== UserConstant.USER_VIP_NO) {
            // 不需要VIP，放行
            return joinPoint.proceed();
        }
        // 以下为：必须是VIP才通过
        if (user==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"请先登录");
        }
        System.out.println(mustVIP);
        System.out.println("当前用户是否是VIP："+user.getIsVip());
        if (mustVIP== UserConstant.USER_VIP_YES && user.getIsVip() != UserConstant.USER_VIP_YES) {
            // 要求必须是VIP，但用户不是VIP，拒绝
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"请先开通VIP");
        }
        return joinPoint.proceed();
    }

}

