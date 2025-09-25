package com.zzx.zzxpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import com.zzx.zzxpicturebackend.model.dto.space.SpaceAddRequest;
import com.zzx.zzxpicturebackend.model.dto.space.SpaceEditRequest;
import com.zzx.zzxpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zzx.zzxpicturebackend.model.dto.space.SpaceUpdateRequest;
import com.zzx.zzxpicturebackend.model.enums.SpaceLevelEnum;
import com.zzx.zzxpicturebackend.model.po.Picture;
import com.zzx.zzxpicturebackend.model.po.Space;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.SpaceVO;
import com.zzx.zzxpicturebackend.model.vo.UserVO;
import com.zzx.zzxpicturebackend.service.PictureService;
import com.zzx.zzxpicturebackend.service.SpaceService;
import com.zzx.zzxpicturebackend.mapper.SpaceMapper;
import com.zzx.zzxpicturebackend.service.UserService;
import lombok.Synchronized;
import org.springframework.beans.BeanUtils;
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
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {


    @Resource
    private UserService userService;

    // 创建事务模板
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 添加空间
     *
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @Override
    @Transactional
    public Long addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
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
        this.fillSpaceBySpaceLevel(space);
        space.setUserId(userId);
        // 校验参数
        this.validSpace(space, true);
        // 校验权限，非管理员只能创建普通级别的空间
        if (!userService.isAdmin(loginUser) && spaceAddRequest.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员不能创建高级空间");
        }
        // 控制同一个用户只能创建一个空间 加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 判断用户是否已经有一个空间
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "用户已创建空间");
                // 写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建空间失败");
                return space.getId();
            });
            return newSpaceId;
        }
    }

    /**
     * 校验参数，用于新增或者编辑进行校验
     *
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, boolean add) {
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Long userId = space.getUserId();
        // 如果是创建数据
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间等级不能为空");
        }
        // 如果是修改数据，并且是修改空间等级
        ThrowUtils.throwIf(spaceLevel != null && spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间等级不存在");
        // 如果是修改数据，并且是修改空间名称
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30, ErrorCode.PARAMS_ERROR, "空间名称不能为空");
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
        Long userId = spaceVO.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUserVO(userVO);
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
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
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
            spaceVO.setUserVO(userService.getUserVO(user));
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
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
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
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(!(user.getId().equals(space.getUserId()) || user.getUserRole().equals("admin")), ErrorCode.NO_AUTH_ERROR);
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
        this.validSpace(space, false);
        // 操作数据库
        boolean result = this.updateById(space);
        return result;
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
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(!(user.getId().equals(oldSpace.getUserId()) || "admin".equals(user.getUserRole())), ErrorCode.NO_AUTH_ERROR);
        // 把dto转换成po
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        // 自动填充数据
        this.fillSpaceBySpaceLevel(space);
        space.setEditTime(new Date());
        // 数据校验
        this.validSpace(space, false);
        // 操作数据库
        boolean result = this.updateById(space);
        return result;

    }
}




