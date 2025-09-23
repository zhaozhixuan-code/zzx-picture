package com.zzx.zzxpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpaceLevelEnum {
    COMMON("普通版", 0, 100L, 100L * 1024 * 1024),
    PROFESSIONAL("专业版", 1, 1000L, 1000L * 1024 * 1024),
    FLAGSHIP("旗舰版", 2, 10000L, 10000L * 1024 * 1024);

    private final String text;
    private final int value;
    private final Long maxCount;
    private final Long maxSize;

    /**
     * 枚举构造
     *
     * @param text     枚举的文本
     * @param value    枚举的值
     * @param maxCount 图片数量限制
     * @param maxSize  图片大小限制
     */
    SpaceLevelEnum(String text, int value, Long maxCount, Long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static SpaceLevelEnum getEnumByValue(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // 遍历所有的枚举，找到匹配的枚举
        for (SpaceLevelEnum anEnum : SpaceLevelEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
