package com.zzx.zzxpicturebackend.model.dto.spaceuser;


import lombok.Data;

import java.io.Serializable;

/**
 * 编辑空间成员请求
 */
@Data
public class SpaceUserUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;
}
