package com.zzx.zzxpicturebackend.model.dto.user;


import lombok.Data;

import java.io.Serializable;

/**
 * 用户编辑请求
 */
@Data
public class UserEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long id;

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
     * 密码
     */
    private String userPassword;


}
