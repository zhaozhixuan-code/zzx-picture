package com.zzx.zzxpicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zzx.zzxpicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.zzx.zzxpicturebackend.manager.auth.model.SpaceUserRole;
import com.zzx.zzxpicturebackend.constant.SpaceUserPermissionConstant;
import com.zzx.zzxpicturebackend.model.enums.SpaceRoleEnum;
import com.zzx.zzxpicturebackend.model.enums.SpaceTypeEnum;
import com.zzx.zzxpicturebackend.model.po.Space;
import com.zzx.zzxpicturebackend.model.po.SpaceUser;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.service.SpaceUserService;
import com.zzx.zzxpicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 加载配置文件到对象
 * 获取角色权限列表
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     *
     * @param spaceUserRole 角色标识
     * @return 权限列表
     */
    public static List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isEmpty(spaceUserRole)) {
            return new ArrayList<>();
        }
        // 获取角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles()
                .stream()
                .filter(r -> r.getKey().equals(spaceUserRole))
                .findFirst()
                .orElse(null);
        // 获取角色的权限列表
        return role == null ? new ArrayList<>() : role.getPermissions();
    }

    /**
     * 根据空间和用户获取权限列表
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

}
