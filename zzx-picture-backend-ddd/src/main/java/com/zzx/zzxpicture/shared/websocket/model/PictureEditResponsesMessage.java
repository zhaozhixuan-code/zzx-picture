package com.zzx.zzxpicture.shared.websocket.model;

import com.zzx.zzxpicture.interfaces.vo.user.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑响应消息
 * 响应给前端的类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditResponsesMessage {

    /**
     * 消息类型：ENTER_EDIT - EDIT_ACTION - EXIT_EDIT
     */
    private String type;

    /**
     * 信息
     */
    private String message;

    /**
     * 编辑动作：放大，缩小，左旋，右旋
     * 需要广播给协同者
     */
    private String editAction;

    /**
     * 编辑该照片的用户信息
     */
    private UserVO user;
}
