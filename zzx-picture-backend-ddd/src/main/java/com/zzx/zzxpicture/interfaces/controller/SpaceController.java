package com.zzx.zzxpicture.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.zzxpicture.infrastructure.annotation.AuthCheck;
import com.zzx.zzxpicture.infrastructure.common.BaseResponse;
import com.zzx.zzxpicture.infrastructure.common.ResultUtils;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceAddRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceEditRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceQueryRequest;
import com.zzx.zzxpicture.interfaces.dto.space.SpaceUpdateRequest;
import com.zzx.zzxpicture.domain.space.valueobject.SpaceLevelEnum;
import com.zzx.zzxpicture.domain.user.valueobject.UserRoleEnum;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.interfaces.vo.space.SpaceLevel;
import com.zzx.zzxpicture.interfaces.vo.space.SpaceVO;
import com.zzx.zzxpicture.application.service.SpaceApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间控制器
 */
@RestController
@RequestMapping("/space")
@Slf4j
public class SpaceController {

    @Resource
    private SpaceApplicationService spaceApplicationService;

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceApplicationService.addSpace(spaceAddRequest, request);
        return ResultUtils.success(spaceId);
    }


    /**
     * 删除空间
     *
     * @param id 空间id
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody Long id, HttpServletRequest request) {
        Boolean result = spaceApplicationService.deleteSpace(id, request);
        return ResultUtils.success(result);
    }

    /**
     * 修改空间（管理员）
     *
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        Boolean result = spaceApplicationService.updateSpace(spaceUpdateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 修改空间（用户）
     *
     * @param spaceEditRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        Boolean result = spaceApplicationService.editSpace(spaceEditRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 获取空间信息 （管理员）
     *
     * @param id 空间id
     * @return 空间信息
     */
    @GetMapping("/get")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Space> getSpaceById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    /**
     * 获取空间信息 （用户）
     *
     * @param id 空间id
     * @return 空间信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        Space space = spaceApplicationService.getById(id);
        SpaceVO spaceVO = spaceApplicationService.getSpaceVO(space, request);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 获取空间列表（管理员）
     *
     * @param spaceQueryRequest 空间查询参数
     * @return 空间列表
     */
    @PostMapping("/list/page")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        Page<Space> spaceList = spaceApplicationService.page(new Page<>(current, size), spaceApplicationService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spaceList);
    }

    /**
     * 获取空间列表
     *
     * @param spaceQueryRequest 空间查询参数
     * @return 空间列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 分页查询
        Page<Space> page = spaceApplicationService.page(new Page<>(current, size), spaceApplicationService.getQueryWrapper(spaceQueryRequest));
        Page<SpaceVO> spaceVOList = spaceApplicationService.getSpaceVOPage(page, request);
        return ResultUtils.success(spaceVOList);
    }


    /**
     * 获取空间等级列表
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> levelList = Arrays.stream(SpaceLevelEnum.values()).map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(levelList);
    }

}
