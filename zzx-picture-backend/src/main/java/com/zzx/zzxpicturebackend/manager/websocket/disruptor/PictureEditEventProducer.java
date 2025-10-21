package com.zzx.zzxpicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.zzx.zzxpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.zzx.zzxpicturebackend.model.po.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

/**
 * 图片编辑事件生产者
 */
@Component
@Slf4j
public class PictureEditEventProducer {

    /**
     * 图片编辑事件Disruptor实例
     */
    @Resource
    Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    /**
     * 发送图片编辑事件
     * @param pictureEditRequestMessage
     * @param webSocketSession
     * @param user
     * @param pictureId
     */
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession webSocketSession,
                             User user, Long pictureId) {
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        // 获取下一个可用位置
        long next = ringBuffer.next();
        try {
            // 获取该位置的元素
            PictureEditEvent pictureEditEvent = ringBuffer.get(next);
            // 设置事件数据
            pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
            pictureEditEvent.setWebSocketSession(webSocketSession);
            pictureEditEvent.setUser(user);
            pictureEditEvent.setPictureId(pictureId);
        } finally {
            // 发布事件
            ringBuffer.publish(next);
        }
    }

    /**
     * 销毁图片编辑事件生产者
     */
    @PreDestroy
    public void destroy() {
        log.info("图片编辑事件生产者销毁");
        pictureEditEventDisruptor.shutdown();
    }
}
