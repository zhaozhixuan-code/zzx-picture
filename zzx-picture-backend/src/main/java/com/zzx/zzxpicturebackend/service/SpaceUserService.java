package com.zzx.zzxpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzx.zzxpicturebackend.common.DeleteRequest;
import com.zzx.zzxpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.zzx.zzxpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zzx.zzxpicturebackend.model.dto.spaceuser.SpaceUserUpdateRequest;
import com.zzx.zzxpicturebackend.model.po.Picture;
import com.zzx.zzxpicturebackend.model.po.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.zzxpicturebackend.model.vo.SpaceUserVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 28299
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-10-10 22:58:09
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 删除空间成员
     *
     * @param deleteRequest
     * @return
     */
    Boolean deleteSpaceUser(DeleteRequest deleteRequest);

    /**
     * 修改空间成员
     *
     * @param spaceUserUpdateRequest
     * @return
     */
    Boolean updateSpaceUser(SpaceUserUpdateRequest spaceUserUpdateRequest);

    /**
     * 获取空间成员视图信息（单条）
     *
     * @param spaceUser 空间成员
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员视图信息（列表）
     *
     * @param spaceUserList 空间成员列表
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
