package com.zzx.zzxpicture.shared.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑请求消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditRequestMessage {

    /**
     * 消息类型：ENTER_EDIT - EDIT_ACTION - EXIT_EDIT
     */
    private String type;

    /**
     * 编辑动作：放大，缩小，左旋，右旋
     * 需要广播给协同者
     */
    private String editAction;

}
