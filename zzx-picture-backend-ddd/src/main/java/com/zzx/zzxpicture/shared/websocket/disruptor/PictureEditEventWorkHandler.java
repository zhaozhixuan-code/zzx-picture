package com.zzx.zzxpicture.shared.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.zzx.zzxpicture.shared.websocket.model.PictureEditMessageTypeEnum;
import com.zzx.zzxpicture.shared.websocket.model.PictureEditRequestMessage;
import com.zzx.zzxpicture.shared.websocket.PictureEditHandler;
import com.zzx.zzxpicture.shared.websocket.model.PictureEditResponsesMessage;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 事件定义的消费者
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserApplicationService userApplicationService;


    /**
     * 处理事件
     *
     * @param pictureEditEvent 事件对象
     * @throws Exception
     */
    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        // 获取事件中的数据
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getWebSocketSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        PictureEditMessageTypeEnum pictureEditTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(pictureEditRequestMessage.getType());

        // 根据对应的消息处理方法
        switch (pictureEditTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                // 发送编辑错误信息
                PictureEditResponsesMessage pictureEditResponsesMessage = new PictureEditResponsesMessage();
                pictureEditResponsesMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponsesMessage.setMessage("消息类型错误");
                pictureEditResponsesMessage.setUser(userApplicationService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponsesMessage)));
        }
    }
}
