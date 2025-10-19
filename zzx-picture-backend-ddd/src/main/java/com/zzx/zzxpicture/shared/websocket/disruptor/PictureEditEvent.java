package com.zzx.zzxpicture.shared.websocket.disruptor;

import com.zzx.zzxpicture.shared.websocket.model.PictureEditRequestMessage;
import com.zzx.zzxpicture.domain.user.entity.User;
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
