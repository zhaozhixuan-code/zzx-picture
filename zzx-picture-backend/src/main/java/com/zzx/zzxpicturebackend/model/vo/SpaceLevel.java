package com.zzx.zzxpicturebackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间等级视图
 */
@Data
@AllArgsConstructor
public class SpaceLevel implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 等级值
     */
    private int value;

    /**
     * 等级名称
     */
    private String text;

    /**
     * 最大数量
     */
    private long maxCount;

    /**
     * 最大空间大小
     */
    private long maxSize;
}
