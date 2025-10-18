package com.zzx.zzxpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum PictureReviewEnum {
    REVIEWING("待审核", 0),
    PASS("审核通过", 1),
    REJECT("审核拒绝", 2);

    private final String text;
    private final int value;

    PictureReviewEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static PictureReviewEnum getEnumByValue(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // 遍历所有的枚举，找到匹配的枚举
        for (PictureReviewEnum anEnum : PictureReviewEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
