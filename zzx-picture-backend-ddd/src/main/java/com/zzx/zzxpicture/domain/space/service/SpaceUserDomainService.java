package com.zzx.zzxpicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.zzxpicture.domain.space.entity.SpaceUser;
import com.zzx.zzxpicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;

/**
 * @author 28299
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-10-10 22:58:09
 */
public interface SpaceUserDomainService extends IService<SpaceUser> {

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
