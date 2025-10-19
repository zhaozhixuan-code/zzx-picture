package com.zzx.zzxpicture.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.zzxpicture.domain.space.service.SpaceDomainService;
import com.zzx.zzxpicture.shared.auth.SpaceUserAuthManager;
import com.zzx.zzxpicture.infrastructure.exception.BusinessException;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.shared.sharding.DynamicShardingManager;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceAddRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceEditRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceQueryRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceUpdateRequest;
import com.zzx.zzxpicture.domain.space.valueobject.SpaceLevelEnum;
import com.zzx.zzxpicture.domain.space.valueobject.SpaceRoleEnum;
import com.zzx.zzxpicture.domain.space.valueobject.SpaceTypeEnum;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.domain.space.entity.SpaceUser;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.interfaces.vo.space.SpaceVO;
import com.zzx.zzxpicture.interfaces.vo.user.UserVO;
import com.zzx.zzxpicture.application.service.SpaceApplicationService;
import com.zzx.zzxpicture.infrastructure.mapper.SpaceMapper;
import com.zzx.zzxpicture.application.service.SpaceUserApplicationService;
import com.zzx.zzxpicture.application.service.UserApplicationService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 28299
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-09-22 21:22:17
 */
@Service
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceApplicationService {


    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    private UserApplicationService userApplicationService;

    // 创建事务模板
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    @Lazy
    private DynamicShardingManager dynamicShardingManager;

    /**
     * 添加空间（个人空间 - 团队空间）
     *
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @Override
    @Transactional
    public Long addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 填充参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 设置默认值
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 默认新增个人空间
        if (spaceAddRequest.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        space.setUserId(userId);
        // 校验参数
        space.validSpace(true);
        // 校验权限，非管理员只能创建普通级别的空间
        if (!loginUser.isAdmin() && spaceAddRequest.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员不能创建高级空间");
        }
        // 控制同一个用户只能创建一个空间 加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 判断用户是否已经有一个空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个");
                // 写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建空间失败");
                // 如果创建的是团队空间，则需要把用户自己添加到空间成员中
                if (spaceAddRequest.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserApplicationService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "添加空间成员失败");
                }
                // 动态创建分表（仅对团队空间生效）
                dynamicShardingManager.createSpacePictureTable(space);
                return space.getId();
            });
            return newSpaceId;
        }
    }


    /**
     * 获取空间视图（单条）
     *
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 转换成包装类
        SpaceVO spaceVO = SpaceVO.poToVo(space);
        // 获取用户信息
        // Long userId = spaceVO.getUserId();
        User loginUser = userApplicationService.getLoginUser(request);
        Long userId = loginUser.getId();
        if (userId != null) {
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            spaceVO.setUserVO(userVO);
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, user);
            spaceVO.setPermissionList(permissionList);
        }
        return spaceVO;
    }


    /**
     * 获取空间视图列表
     *
     * @param spacePage 个人空间分页
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        // 获取空间列表
        List<Space> spaceList = spacePage.getRecords();
        // 创建VO分页对象
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        // 封装VO列表：将Space对象转换为SpaceVO对象
        List<SpaceVO> spaceVOList = spaceList.stream().map(space -> {
            SpaceVO spaceVO = new SpaceVO();
            BeanUtil.copyProperties(space, spaceVO);
            return spaceVO;
        }).collect(Collectors.toList());

        //  查询关联用户信息
        // 提取所有空间的用户ID集合
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 根据用户ID批量查询用户信息，并按ID分组
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 为每个空间VO设置用户信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            // 根据用户ID获取对应的用户信息
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            // 设置用户VO信息
            spaceVO.setUserVO(userApplicationService.getUserVO(user));
        });

        // 将转换后的VO列表设置到VO分页对象中
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }


    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    /**
     * 根据空间等级填充空间信息
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }


    /**
     * 删除空间
     *
     * @param id      空间id
     * @param request
     * @return
     */
    @Override
    public Boolean deleteSpace(Long id, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        // 获取空间，判断数据库中是否存在该空间
        Space space = this.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取用户id，仅本人或者管理员才能删除
        User user = userApplicationService.getLoginUser(request);
        checkSpaceAuth(user, space);
        // ThrowUtils.throwIf(!(user.getId().equals(space.getUserId()) || user.getUserRole().equals("admin")), ErrorCode.NO_AUTH_ERROR);
        // 删除空间
        boolean result = this.removeById(id);
        return result;
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
        return spaceDomainService.updateSpace(spaceUpdateRequest, request);
    }

    /**
     * 修改空间（用户）
     *
     * @param spaceEditRequest
     * @param request
     * @return
     */
    @Override
    public Boolean editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(spaceEditRequest == null, ErrorCode.PARAMS_ERROR);
        // 判断空间是否存在
        Space oldSpace = this.getById(spaceEditRequest.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验身份，只有用户本人或者管理员才能编辑
        User user = userApplicationService.getLoginUser(request);
        checkSpaceAuth(user, oldSpace);
        // ThrowUtils.throwIf(!(user.getId().equals(oldSpace.getUserId()) || "admin".equals(user.getUserRole())), ErrorCode.NO_AUTH_ERROR);
        // 把dto转换成po
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        // 自动填充数据
        this.fillSpaceBySpaceLevel(space);
        space.setEditTime(new Date());
        // 数据校验
        space.validSpace(false);
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
        spaceDomainService.checkSpaceAuth(loginUser, space);
    }
}




