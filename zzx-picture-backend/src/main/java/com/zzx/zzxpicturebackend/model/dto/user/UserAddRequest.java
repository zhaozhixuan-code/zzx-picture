package com.zzx.zzxpicturebackend.model.dto.user;


import lombok.Data;


/**
 * 添加用户
 */
@Data
public class UserAddRequest {

    /**
     * 账号
     */
    private String userAccount;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

}
