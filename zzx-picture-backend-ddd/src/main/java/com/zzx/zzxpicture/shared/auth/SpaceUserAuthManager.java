package com.zzx.zzxpicture.shared.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zzx.zzxpicture.shared.auth.model.SpaceUserAuthConfig;
import com.zzx.zzxpicture.shared.auth.model.SpaceUserRole;
import com.zzx.zzxpicture.domain.space.constant.SpaceUserPermissionConstant;
import com.zzx.zzxpicture.domain.space.valueobject.SpaceRoleEnum;
import com.zzx.zzxpicture.domain.space.valueobject.SpaceTypeEnum;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.domain.space.entity.SpaceUser;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.application.service.SpaceUserApplicationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
    @Lazy
    private SpaceUserApplicationService spaceUserApplicationService;


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
            if (loginUser.isAdmin()) {
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
                if (space.getUserId().equals(loginUser.getId()) || loginUser.isAdmin()) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserApplicationService.lambdaQuery()
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
