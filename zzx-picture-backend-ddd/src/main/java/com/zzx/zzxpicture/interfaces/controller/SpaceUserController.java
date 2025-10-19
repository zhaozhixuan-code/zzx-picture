package com.zzx.zzxpicture.interfaces.controller;


import com.zzx.zzxpicture.shared.auth.annotation.SaSpaceCheckPermission;
import com.zzx.zzxpicture.infrastructure.common.BaseResponse;
import com.zzx.zzxpicture.infrastructure.common.DeleteRequest;
import com.zzx.zzxpicture.infrastructure.common.ResultUtils;
import com.zzx.zzxpicture.domain.space.constant.SpaceUserPermissionConstant;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.zzx.zzxpicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.zzx.zzxpicture.interfaces.dto.spaceuser.SpaceUserUpdateRequest;
import com.zzx.zzxpicture.domain.space.entity.SpaceUser;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.interfaces.vo.space.SpaceUserVO;
import com.zzx.zzxpicture.application.service.SpaceUserApplicationService;
import com.zzx.zzxpicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private UserApplicationService userApplicationService;


    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = spaceUserApplicationService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 删除空间成员
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        Boolean result = spaceUserApplicationService.deleteSpaceUser(deleteRequest);
        return ResultUtils.success(result);
    }

    /**
     * 修改空间成员信息
     *
     * @param spaceUserUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> updateSpaceUser(@RequestBody SpaceUserUpdateRequest spaceUserUpdateRequest) {
        ThrowUtils.throwIf(spaceUserUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        Boolean result = spaceUserApplicationService.updateSpaceUser(spaceUserUpdateRequest);
        return ResultUtils.success(result);
    }


    /**
     * 获取空间成员视图列表
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 查询空间成员
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        List<SpaceUserVO> spaceUserVOList = spaceUserApplicationService.getSpaceUserVOList(spaceUserList);
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 获取当前用户空加入的空间
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        List<SpaceUserVO> spaceUserVOList = spaceUserApplicationService.getSpaceUserVOList(spaceUserList);
        return ResultUtils.success(spaceUserVOList);
    }
}
