package com.zzx.zzxpicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceAddRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceEditRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceQueryRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceUpdateRequest;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.interfaces.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 28299
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-09-22 21:22:17
 */
public interface SpaceApplicationService extends IService<Space> {

    /**
     * 添加空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request);


    /**
     * 获取空间视图 （单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);




    /**
     * 获取空间视图列表（脱敏 + 用户信息）
     *
     * @param spacePage 个人空间分页
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

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
     * 删除空间
     * @param id
     * @param request
     * @return
     */
    Boolean deleteSpace(Long id, HttpServletRequest request);

    /**
     * 修改空间（管理员）
     * @param spaceUpdateRequest
     * @param request
     * @return
     */
    Boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request);


    /**
     * 编辑空间（用户）
     * @param spaceEditRequest
     * @param request
     * @return
     */
    Boolean editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request);

    /**
     * 校验权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}