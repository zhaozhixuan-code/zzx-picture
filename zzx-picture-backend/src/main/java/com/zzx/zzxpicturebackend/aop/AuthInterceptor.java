package com.zzx.zzxpicturebackend.aop;


import com.zzx.zzxpicturebackend.annotation.AuthCheck;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.model.enums.UserRoleEnum;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 鉴权拦截器
 */
@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 切点
     */
    @Pointcut("execution(* com.zzx.zzxpicturebackend.controller.*.*(..)) &&  @annotation(com.zzx.zzxpicturebackend.annotation.AuthCheck)")
    public void pointCut() {
    }

    /**
     * 前置通知
     */
    @Before("pointCut()")
    public void doInterceptor(JoinPoint joinPoint) {

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User user = userService.getLoginUser(request);
        // 获取用户的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(user.getUserRole());
        // 利用反射获取方法上的访问权限
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        UserRoleEnum mustRole = signature.getMethod().getAnnotation(AuthCheck.class).value();
        // 如果访问权限为用户，表示都可以访问，直接放行
        if (mustRole == UserRoleEnum.USER || mustRole == null) {
            return;
        }

        // 拒绝访问情况
        // 用户权限为null
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 如果要求为管理员权限，但是用户没有权限，则拒绝
        if (userRoleEnum == UserRoleEnum.USER && mustRole == UserRoleEnum.ADMIN) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}
