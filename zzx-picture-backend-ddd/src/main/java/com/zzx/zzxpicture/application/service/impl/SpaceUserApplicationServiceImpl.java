package com.zzx.zzxpicture.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.zzxpicture.application.service.SpaceUserApplicationService;
import com.zzx.zzxpicture.domain.space.service.SpaceUserDomainService;
import com.zzx.zzxpicture.infrastructure.common.DeleteRequest;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.zzx.zzxpicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.zzx.zzxpicture.interfaces.dto.spaceuser.SpaceUserUpdateRequest;
import com.zzx.zzxpicture.domain.space.valueobject.SpaceRoleEnum;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.domain.space.entity.SpaceUser;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.interfaces.vo.space.SpaceUserVO;
import com.zzx.zzxpicture.interfaces.vo.space.SpaceVO;
import com.zzx.zzxpicture.interfaces.vo.user.UserVO;
import com.zzx.zzxpicture.application.service.SpaceApplicationService;
import com.zzx.zzxpicture.infrastructure.mapper.SpaceUserMapper;
import com.zzx.zzxpicture.application.service.UserApplicationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 28299
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-10-10 22:58:09
 */
@Service
public class SpaceUserApplicationServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserApplicationService {


    @Resource
    private SpaceUserDomainService spaceUserDomainService;

    @Resource
    @Lazy
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);

        // 如果没有传递空间角色默认为浏览者
        if (spaceUser.getSpaceRole() == null) {
            spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
        }
        // 校验参数
        validSpace(spaceUser, true);
        // 存入数据库
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "添加空间成员失败");
        return spaceUser.getId();
    }

    /**
     * 删除空间成员
     *
     * @param deleteRequest
     * @return
     */
    @Override
    public Boolean deleteSpaceUser(DeleteRequest deleteRequest) {
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        // 查询空间成员是否存在
        SpaceUser spaceUser = this.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
        boolean remove = this.removeById(id);
        return remove;
    }

    /**
     * 修改空间成员
     *
     * @param spaceUserUpdateRequest
     * @return
     */
    @Override
    public Boolean updateSpaceUser(SpaceUserUpdateRequest spaceUserUpdateRequest) {
        // 参数转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserUpdateRequest, spaceUser);
        // 判断空间用户是否存在
        Long id = spaceUser.getId();
        SpaceUser oldSpaceUser = this.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
        // 参数校验
        validSpace(spaceUser, false);
        boolean result = this.updateById(spaceUser);
        return result;
    }

    /**
     * 获取空间成员视图信息（单条）
     *
     * @param spaceUser 空间成员
     * @return
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtil.copyProperties(spaceUser, spaceUserVO);
        // 设置用户信息
        Long userId = spaceUser.getUserId();
        User user = userApplicationService.getUserById(userId);
        UserVO userVO = userApplicationService.getUserVO(user);
        spaceUserVO.setUser(userVO);
        // 设置空间信息
        Long spaceId = spaceUser.getSpaceId();
        Space space = spaceApplicationService.getById(spaceId);
        SpaceVO spaceVO = spaceApplicationService.getSpaceVO(space, request);
        spaceUserVO.setSpace(spaceVO);
        return spaceUserVO;
    }

    /**
     * 获取空间成员视图信息（列表）
     *
     * @param spaceUserList 空间成员列表
     * @return
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 1、如果为空直接返回空列表
        if (spaceUserList == null || spaceUserList.isEmpty()) {
            return List.of();
        }
        // 2、封装返回对象
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::poToVo).collect(Collectors.toList());

        // 3、现获取所有的用户 id 和空间 id 的 set对象
        Set<Long> userIdsSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdsSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 4、批量查询用户 id 对应的用户和空间 id 对应的空间map对象
        Map<Long, User> userMap = userApplicationService.listByIds(userIdsSet).stream().collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, Space> spaceMap = spaceApplicationService.listByIds(spaceIdsSet).stream().collect(Collectors.toMap(Space::getId, space -> space));
        // 5、批量填充用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            if (userMap.containsKey(userId)) {
                UserVO userVO = userApplicationService.getUserVO(userMap.get(userId));
                spaceUserVO.setUser(userVO);
            }
            // 填充空间信息
            if (spaceMap.containsKey(spaceId)) {
                SpaceVO spaceVO = SpaceVO.poToVo(spaceMap.get(spaceId));
                spaceUserVO.setSpace(spaceVO);
            }
        });
        return spaceUserVOList;
    }


    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        return spaceUserDomainService.getQueryWrapper(spaceUserQueryRequest);
    }

    /**
     * 校验参数
     *
     * @param spaceUser
     * @param add       是否为添加 true 为添加  false 为修改
     */
    private void validSpace(SpaceUser spaceUser, boolean add) {
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        // 如果是添加成员空间
        if (add) {
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            User user = userApplicationService.getUserById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "不存在该用户");
        }
        // 如果是编辑成员空间 则为修改成员角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum enumByValue = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }


}




