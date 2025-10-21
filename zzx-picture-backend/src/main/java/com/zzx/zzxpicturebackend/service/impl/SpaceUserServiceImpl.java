package com.zzx.zzxpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.zzxpicturebackend.common.DeleteRequest;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import com.zzx.zzxpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.zzx.zzxpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zzx.zzxpicturebackend.model.dto.spaceuser.SpaceUserUpdateRequest;
import com.zzx.zzxpicturebackend.model.enums.SpaceRoleEnum;
import com.zzx.zzxpicturebackend.model.po.Picture;
import com.zzx.zzxpicturebackend.model.po.Space;
import com.zzx.zzxpicturebackend.model.po.SpaceUser;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.SpaceUserVO;
import com.zzx.zzxpicturebackend.model.vo.SpaceVO;
import com.zzx.zzxpicturebackend.model.vo.UserVO;
import com.zzx.zzxpicturebackend.service.SpaceService;
import com.zzx.zzxpicturebackend.service.SpaceUserService;
import com.zzx.zzxpicturebackend.mapper.SpaceUserMapper;
import com.zzx.zzxpicturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Resource
    private UserService userService;

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
        User user = userService.getById(userId);
        UserVO userVO = userService.getUserVO(user);
        spaceUserVO.setUser(userVO);
        // 设置空间信息
        Long spaceId = spaceUser.getSpaceId();
        Space space = spaceService.getById(spaceId);
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
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
        if(spaceUserList == null || spaceUserList.isEmpty()){
            return List.of();
        }
        // 2、封装返回对象
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::poToVo).collect(Collectors.toList());

        // 3、现获取所有的用户 id 和空间 id 的 set对象
        Set<Long> userIdsSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdsSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 4、批量查询用户 id 对应的用户和空间 id 对应的空间map对象
        Map<Long, User> userMap = userService.listByIds(userIdsSet).stream().collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, Space> spaceMap = spaceService.listByIds(spaceIdsSet).stream().collect(Collectors.toMap(Space::getId, space -> space));
        // 5、批量填充用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            if(userMap.containsKey(userId)){
                UserVO userVO = userService.getUserVO(userMap.get(userId));
                spaceUserVO.setUser(userVO);
            }
            // 填充空间信息
            if(spaceMap.containsKey(spaceId)){
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
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();

        // 从多字段中搜索
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
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
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "不存在该用户");
        }
        // 如果是编辑成员空间 则为修改成员角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum enumByValue = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }


}




