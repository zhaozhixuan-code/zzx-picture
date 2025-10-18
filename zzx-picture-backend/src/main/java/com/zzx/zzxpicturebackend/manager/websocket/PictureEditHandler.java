package com.zzx.zzxpicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.zzx.zzxpicturebackend.manager.websocket.disruptor.PictureEditEvent;
import com.zzx.zzxpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.zzx.zzxpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.zzx.zzxpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.zzx.zzxpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.zzx.zzxpicturebackend.manager.websocket.model.PictureEditResponsesMessage;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.UserVO;
import com.zzx.zzxpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片编辑处理
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * 每张照片编辑状态存储
     * key: 图片ID
     * value: 正在编辑的用户ID
     */
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    /**
     * 每张图片的WebSocket会话存储
     * key: 图片ID
     * value: 图片的WebSocket会话集合
     */
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();


    /**
     * 新连接建立
     *
     * @param session WebSocket会话对象，表示新建立的客户端连接
     * @throws Exception 当连接建立过程中出现异常时抛出
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("新连接建立: {}", session.getId());
        // 获取webSocketSession中的信息
        User user = (User) session.getAttributes().get("user");
        Long userId = (Long) session.getAttributes().get("userId");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        Long spaceId = (Long) session.getAttributes().get("spaceId");
        // 保存会话到集合中
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        // 构造响应
        PictureEditResponsesMessage pictureEditResponsesMessage = new PictureEditResponsesMessage();
        pictureEditResponsesMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponsesMessage.setMessage(String.format("%s 加入编辑", user.getUserName()));
        UserVO userVO = userService.getUserVO(user);
        pictureEditResponsesMessage.setUser(userVO);
        // 广播给同一张照片的用户
        broadcast(pictureId, pictureEditResponsesMessage);
    }

    /**
     * 处理图片编辑请求
     *
     * @param session WebSocket会话对象，用于与客户端进行通信
     * @param message 接收到的文本消息对象，PictureEditResponsesMessage
     * @throws IOException 当消息发送或处理出现IO异常时抛出
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 获取相关参数
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        if (pictureEditTypeEnum == null) {
            // 构造响应
            PictureEditResponsesMessage pictureEditResponsesMessage = new PictureEditResponsesMessage();
            pictureEditResponsesMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
            pictureEditResponsesMessage.setMessage("消息类型错误");
            pictureEditResponsesMessage.setUser(userService.getUserVO(user));
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponsesMessage)));
            return;
        }
        // 生产消息
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
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
        log.info("连接关闭: {} ，状态: {}", session.getId(), status);
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");
        // 移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        // 移除当前用户的会话
        pictureSessions.get(pictureId).remove(session);

        // 响应
        PictureEditResponsesMessage pictureEditResponsesMessage = new PictureEditResponsesMessage();
        pictureEditResponsesMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponsesMessage.setMessage(String.format("%s 退出编辑", user.getUserName()));
        pictureEditResponsesMessage.setUser(userService.getUserVO(user));
        broadcast(pictureId, pictureEditResponsesMessage);
    }


    /**
     * 处理图片进入编辑
     *
     * @param pictureEditRequestMessage 接收到的图片进入编辑请求消息对象
     * @param session                   WebSocket会话对象，表示当前处理图片进入编辑的WebSocket会话
     * @param user                      当前用户对象
     * @param pictureId                 当前图片ID
     * @throws IOException 当消息发送或处理出现IO异常时抛出
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                       User user, Long pictureId) throws IOException {
        // 没有用户正在编辑图片，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置当前用户为正在编辑图片的用户
            pictureEditingUsers.put(pictureId, user.getId());
            PictureEditResponsesMessage pictureEditResponsesMessage = new PictureEditResponsesMessage();
            pictureEditResponsesMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            pictureEditResponsesMessage.setMessage(String.format("%s开始编辑", user.getUserName()));
            pictureEditResponsesMessage.setUser(userService.getUserVO(user));
            broadcast(pictureId, pictureEditResponsesMessage);
        } else {
            // 存在用户正在编辑图片
            PictureEditResponsesMessage pictureEditResponsesMessage = new PictureEditResponsesMessage();
            pictureEditResponsesMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
            Long userId = pictureEditingUsers.get(pictureId);
            User editingUser = userService.getById(userId);
            pictureEditResponsesMessage.setMessage(String.format("用户%s正在编辑", editingUser.getUserName()));
            pictureEditResponsesMessage.setUser(userService.getUserVO(user));
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponsesMessage)));
        }

    }

    /**
     * 处理图片编辑动作
     *
     * @param pictureEditRequestMessage 接收到的图片编辑请求消息对象
     * @param session                   WebSocket会话对象，表示当前处理图片编辑动作的WebSocket会话
     * @param user                      当前用户对象
     * @param pictureId                 当前图片ID
     * @throws IOException 当消息发送或处理出现IO异常时抛出
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                        User user, Long pictureId) throws IOException {
        // 校验编辑动作类型
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        // 如果不是有效的编辑动作类型
        if (actionEnum == null) {
            PictureEditResponsesMessage pictureEditResponsesMessage = new PictureEditResponsesMessage();
            pictureEditResponsesMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
            pictureEditResponsesMessage.setMessage("编辑动作类型错误");
            pictureEditResponsesMessage.setUser(userService.getUserVO(user));
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponsesMessage)));
        }
        Long editingUserId = pictureEditingUsers.get(pictureId);
        // 确认是当前编辑者才能编辑
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponsesMessage pictureEditResponsesMessage = new PictureEditResponsesMessage();
            pictureEditResponsesMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponsesMessage.setEditAction(editAction);
            pictureEditResponsesMessage.setUser(userService.getUserVO(user));
            pictureEditResponsesMessage.setMessage(String.format("%s对照片进行%s操作", user.getUserName(), actionEnum.getText()));
            // 广播给其他除了自己用户
            broadcast(pictureId, pictureEditResponsesMessage, session);
        }
    }


    /**
     * 处理图片退出编辑
     *
     * @param pictureEditRequestMessage 接收到的图片退出编辑请求消息对象
     * @param session                   WebSocket会话对象，表示当前处理图片退出编辑的WebSocket会话
     * @param user                      当前用户对象
     * @param pictureId                 当前图片ID
     * @throws IOException 当消息发送或处理出现IO异常时抛出
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                      User user, Long pictureId) throws IOException {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除编辑用户
            pictureEditingUsers.remove(pictureId);
            // 构造相应发送编辑退出的信息
            PictureEditResponsesMessage pictureEditResponsesMessage = new PictureEditResponsesMessage();
            pictureEditResponsesMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponsesMessage.setMessage(String.format("%s退出编辑", user.getUserName()));
            pictureEditResponsesMessage.setUser(userService.getUserVO(user));
            broadcast(pictureId, pictureEditResponsesMessage);
        }
    }


    /**
     * 广播消息
     *
     * @param pictureId                   图片ID
     * @param pictureEditResponsesMessage 广播的消息
     * @param excludeSession              需要排除的WebSocket会话对象
     */
    private void broadcast(Long pictureId, PictureEditResponsesMessage pictureEditResponsesMessage,
                           WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 处理 Long 类型精度丢失
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            // 序列化为JSON字符串
            String messageJson = objectMapper.writeValueAsString(pictureEditResponsesMessage);
            TextMessage textMessage = new TextMessage(messageJson);
            for (WebSocketSession session : sessionSet) {
                // 移除掉为null的会话
                if (excludeSession != null && session.equals(excludeSession)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播消息 广播会话给所有人
     *
     * @param pictureId                   图片ID
     * @param pictureEditResponsesMessage 广播的消息
     */
    private void broadcast(Long pictureId, PictureEditResponsesMessage pictureEditResponsesMessage) throws IOException {
        this.broadcast(pictureId, pictureEditResponsesMessage, null);
    }

}