package com.zzx.zzxpicturebackend.manager.websocket;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.mchange.lang.LongUtils;
import com.zzx.zzxpicturebackend.constant.SpaceUserPermissionConstant;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import com.zzx.zzxpicturebackend.manager.auth.SpaceUserAuthManager;
import com.zzx.zzxpicturebackend.model.enums.SpaceTypeEnum;
import com.zzx.zzxpicturebackend.model.po.Picture;
import com.zzx.zzxpicturebackend.model.po.Space;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.service.PictureService;
import com.zzx.zzxpicturebackend.service.SpaceService;
import com.zzx.zzxpicturebackend.service.UserService;
import com.zzx.zzxpicturebackend.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * WebSocket 握手拦截器
 */
@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {


    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 在WebSocket握手之前执行，可以用来验证用户是否有团队空间编辑的权限，否则拒绝连接
     *
     * @param request    来自客户端的握手请求
     * @param response   将要返回给客户端的响应
     * @param wsHandler  处理WebSocket消息的处理器
     * @param attributes 用于存储WebSocket会话属性的映射
     * @return 如果握手应该继续则返回true，否则返回false
     * @throws Exception 如果在握手前处理过程中发生错误
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        // 1. 类型检查和转换
        if (!(request instanceof ServletServerHttpRequest)) {
            log.error("不支持的请求类型，拒绝连接");
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }

        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        try {
            // 2. 校验参数
            // 2.1 获取请求参数
            String pictureId = httpRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("pictureId不能为空，拒绝连接");
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return false;
            }

            // 2.2 获取登录用户
            User loginUser = userService.getLoginUser(httpRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("未登录，拒绝连接");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 3. 校验权限
            // 获取照片，如果照片不存在，拒绝连接
            Picture picture = pictureService.getById(pictureId);
            if (ObjUtil.isEmpty(picture)) {
                log.error("picture不存在，拒绝连接");
                response.setStatusCode(HttpStatus.NOT_FOUND);
                return false;
            }

            // 如果不存在空间 或者不是团队空间拒绝连接
            Long spaceId = picture.getSpaceId();
            if (spaceId == null || spaceId == 0L) {
                log.error("照片在公共图库，拒绝连接");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            Space space = spaceService.getById(spaceId);
            if (ObjUtil.isEmpty(space)) {
                log.error("空间不存在，拒绝连接");
                response.setStatusCode(HttpStatus.NOT_FOUND);
                return false;
            }

            if (!Objects.equals(space.getSpaceType(), SpaceTypeEnum.TEAM.getValue())) {
                log.error("非团队空间，拒绝连接");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            // 获取图片的编辑权限，如果没有该权限，拒绝连接
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (permissionList == null || !permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("没有编辑权限，拒绝连接");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            // 存储用户信息到WebSocket会话
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId",Long.valueOf(pictureId));
            attributes.put("spaceId", spaceId);

            log.info("WebSocket握手验证通过: 用户={}, 图片={}, 空间={}",
                    loginUser.getId(), pictureId, spaceId);
            return true;

        } catch (Exception e) {
            log.error("WebSocket握手验证过程中发生异常", e);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }
    /**
     * 在WebSocket握手之后执行，可以用来清理资源、记录日志等
     *
     * @param request   来自客户端的握手请求
     * @param response  发送给客户端的响应
     * @param wsHandler 处理WebSocket消息的处理器
     * @param exception 握手过程中可能抛出的异常
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
