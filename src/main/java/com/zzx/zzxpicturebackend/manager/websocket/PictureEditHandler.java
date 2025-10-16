package com.zzx.zzxpicturebackend.manager.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * 图片编辑处理
 */
@Component
public class PictureEditHandler extends TextWebSocketHandler {


    /**
     * 处理图片编辑请求
     *
     * @param session WebSocket会话对象，用于与客户端进行通信
     * @param message 接收到的文本消息对象，包含客户端发送的图片编辑指令
     * @throws IOException 当消息发送或处理出现IO异常时抛出
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        System.out.println("收到消息: " + payload);
        session.sendMessage(new TextMessage("服务器响应: " + payload));
    }


    /**
     * 新连接建立
     *
     * @param session WebSocket会话对象，表示新建立的客户端连接
     * @throws Exception 当连接建立过程中出现异常时抛出
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("新连接建立: " + session.getId());
        session.sendMessage(new TextMessage("欢迎加入Spring WebSocket服务！"));
    }


    /**
     * 连接关闭
     *
     * @param session WebSocket会话对象，表示即将关闭的客户端连接
     * @param status  关闭状态信息，包含关闭代码和原因
     * @throws Exception 当连接关闭过程中出现异常时抛出
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("连接关闭: " + session.getId() + "，状态: " + status);
    }

}