package com.zzx.zzxpicture.interfaces.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间请求
 */
@Data
public class SpaceAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别： 1-普通版本 2-专业版 3-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-个人空间 1-团队空间
     */
    private Integer spaceType;

}
