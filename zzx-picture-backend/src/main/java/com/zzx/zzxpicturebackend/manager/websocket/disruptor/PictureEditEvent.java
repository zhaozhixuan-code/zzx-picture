package com.zzx.zzxpicturebackend.manager.websocket.disruptor;

import com.zzx.zzxpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.zzx.zzxpicturebackend.model.po.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件
 */
@Data
public class PictureEditEvent {

    /**
     * 图片编辑请求信息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * WebSocketSession 当前用户的 session
     */
    private WebSocketSession webSocketSession;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 编辑当前图片 id
     */
    private Long pictureId;
}
