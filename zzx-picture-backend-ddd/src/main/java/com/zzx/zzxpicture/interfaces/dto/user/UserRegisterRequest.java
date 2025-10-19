package com.zzx.zzxpicture.interfaces.dto.user;

import lombok.Data;

import java.io.Serializable;


/**
 * 用户注册请求数据传输类
 * 用于封装用户注册时提交的账户信息
 */
@Data
public class UserRegisterRequest implements Serializable {

    /**
     * 用户账户名
     * 用于用户登录时的身份标识
     */
    private String userAccount;

    /**
     * 用户密码
     * 用于用户身份验证的安全凭证
     */
    private String userPassword;

    /**
     * 确认密码
     * 用于验证用户两次输入的密码是否一致
     */
    private String checkPassword;
}
