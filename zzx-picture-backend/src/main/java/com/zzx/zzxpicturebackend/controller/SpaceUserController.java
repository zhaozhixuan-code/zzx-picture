package com.zzx.zzxpicturebackend.controller;


import com.zzx.zzxpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.zzx.zzxpicturebackend.common.BaseResponse;
import com.zzx.zzxpicturebackend.common.DeleteRequest;
import com.zzx.zzxpicturebackend.common.ResultUtils;
import com.zzx.zzxpicturebackend.constant.SpaceUserPermissionConstant;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import com.zzx.zzxpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.zzx.zzxpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zzx.zzxpicturebackend.model.dto.spaceuser.SpaceUserUpdateRequest;
import com.zzx.zzxpicturebackend.model.po.SpaceUser;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.SpaceUserVO;
import com.zzx.zzxpicturebackend.service.SpaceUserService;
import com.zzx.zzxpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;


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
        Long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
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
        Boolean result = spaceUserService.deleteSpaceUser(deleteRequest);
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
        Boolean result = spaceUserService.updateSpaceUser(spaceUserUpdateRequest);
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
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserList);
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 获取当前用户空加入的空间
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserList);
        return ResultUtils.success(spaceUserVOList);
    }
}
