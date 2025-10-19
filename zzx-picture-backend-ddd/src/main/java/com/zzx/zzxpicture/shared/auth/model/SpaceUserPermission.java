package com.zzx.zzxpicture.shared.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间成员权限
 */
@Data
public class SpaceUserPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限标识
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

}
