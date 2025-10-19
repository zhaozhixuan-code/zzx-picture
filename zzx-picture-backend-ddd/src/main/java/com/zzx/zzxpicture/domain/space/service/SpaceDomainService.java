package com.zzx.zzxpicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceAddRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceEditRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceQueryRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceUpdateRequest;
import com.zzx.zzxpicture.interfaces.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 28299
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-09-22 21:22:17
 */
public interface SpaceDomainService extends IService<Space> {


    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 根据空间等级填充空间信息
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);



    /**
     * 修改空间（管理员）
     * @param spaceUpdateRequest
     * @param request
     * @return
     */
    Boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request);



    /**
     * 校验权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}