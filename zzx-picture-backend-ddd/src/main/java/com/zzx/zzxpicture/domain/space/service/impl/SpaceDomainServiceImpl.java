package com.zzx.zzxpicture.domain.space.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.domain.space.repository.SpaceRepository;
import com.zzx.zzxpicture.domain.space.service.SpaceDomainService;
import com.zzx.zzxpicture.domain.space.valueobject.SpaceLevelEnum;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.infrastructure.exception.BusinessException;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.infrastructure.mapper.SpaceMapper;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceQueryRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceUpdateRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author 28299
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-09-22 21:22:17
 */
@Service
public class SpaceDomainServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceDomainService {

    @Resource
    private SpaceRepository spaceRepository;

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 根据空间等级填充空间信息
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if (spaceLevelEnum != null) {
            Long maxSize = spaceLevelEnum.getMaxSize();
            if (maxSize != null) {
                space.setMaxSize(maxSize);
            }
            Long maxCount = spaceLevelEnum.getMaxCount();
            if (maxCount != null) {
                space.setMaxCount(maxCount);
            }
        }

    }


    /**
     * 修改空间（管理员）
     *
     * @param spaceUpdateRequest
     * @param request
     * @return
     */
    @Override
    public Boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(spaceUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // 判断是否存在该空间
        Long id = spaceUpdateRequest.getId();
        Space oldSpace = this.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 把dto转换成po
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        this.fillSpaceBySpaceLevel(space);
        // 数据校验
        space.validSpace( false);
        // 操作数据库
        boolean result = this.updateById(space);
        return result;
    }

    /**
     * 校验空间权限 (仅管理员或者空间创建人可以编辑)
     *
     * @param loginUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅管理员或者空间创建人可以编辑
        if (!space.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}




