package com.zzx.zzxpicture.interfaces.vo.space;


import cn.hutool.core.bean.BeanUtil;
import com.zzx.zzxpicture.interfaces.vo.user.UserVO;
import com.zzx.zzxpicture.domain.space.entity.SpaceUser;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间用户视图
 * 查询某个空间下的用户，以及关联的空间信息和用户信息
 */
@Data
public class SpaceUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 空间信息
     */
    private SpaceVO space;

    /**
     * po 转 vo
     *
     * @param spaceUser
     * @return
     */
    public static SpaceUserVO poToVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtil.copyProperties(spaceUser, spaceUserVO);
        return spaceUserVO;
    }

    /**
     * vo 转 po
     *
     * @param spaceUserVO
     * @return
     */
    public static SpaceUser voToPo(SpaceUserVO spaceUserVO) {
        if (spaceUserVO == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserVO, spaceUser);
        return spaceUser;
    }
}
