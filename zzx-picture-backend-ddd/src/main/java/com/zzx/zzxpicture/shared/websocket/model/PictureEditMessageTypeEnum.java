package com.zzx.zzxpicture.shared.websocket.model;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum PictureEditMessageTypeEnum {

    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),
    ENTER_EDIT("进入编辑", "ENTER_EDIT"),
    EXIT_EDIT("退出编辑", "EXIT_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION");

    private final String text;
    private final String value;

    PictureEditMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static PictureEditMessageTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureEditMessageTypeEnum messageType : PictureEditMessageTypeEnum.values()) {
            if (messageType.value.equals(value)) {
                return messageType;
            }
        }
        return null;
    }

}
