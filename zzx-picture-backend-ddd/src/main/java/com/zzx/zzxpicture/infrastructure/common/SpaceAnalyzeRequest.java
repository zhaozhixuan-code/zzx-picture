package com.zzx.zzxpicture.infrastructure.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用空间分析请求参数
 * <p>
 * queryAll 字段：为true时表示查询全空间，仅管理员可用
 * <p>
 * queryPublic字段：为true时表示查询公共空间图库，仅管理员可用
 * <p>
 * spaceId 字段： 尽在queryAll 和 queryPublic 均为false 使使用，查询个人空间，仅管理员和空间创建者可用
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 全空间分析
     */
    private boolean queryAll;

}
