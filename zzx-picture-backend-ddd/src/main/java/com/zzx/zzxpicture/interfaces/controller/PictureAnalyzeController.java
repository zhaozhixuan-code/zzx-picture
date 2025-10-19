package com.zzx.zzxpicture.interfaces.controller;

import com.zzx.zzxpicture.infrastructure.common.BaseResponse;
import com.zzx.zzxpicture.infrastructure.common.ResultUtils;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.interfaces.dto.analyze.*;
import com.zzx.zzxpicture.interfaces.vo.space.analyze.*;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.application.service.PictureApplicationService;
import com.zzx.zzxpicture.application.service.SpaceAnalyzeApplicationService;
import com.zzx.zzxpicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间分析
 */
@RestController
@RequestMapping("/picture/analyze")
@Slf4j
public class PictureAnalyzeController {

    @Resource
    private PictureApplicationService pictureApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceAnalyzeApplicationService spaceAnalyzeApplicationService;

    /**
     * 获取空间使用情况
     *
     * @param spaceUsageAnalyzeRequest 空间分析参数，查询某个空间
     * @param request                  请求
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getUsage(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeApplicationService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    /**
     * 获取空间中图片分类使用情况
     *
     * @param spaceCategoryAnalyzeRequest 获取空间图片分类情况参数
     * @param request                     请求
     * @return
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getCategory(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze = spaceAnalyzeApplicationService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceCategoryAnalyze);
    }

    /**
     * 获取空间中图片标签使用情况
     *
     * @param spaceTagAnalyzeRequest 获取空间图片标签情况参数
     * @param request                请求
     * @return
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getTag(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        List<SpaceTagAnalyzeResponse> spaceTagAnalyze = spaceAnalyzeApplicationService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceTagAnalyze);
    }

    /**
     * 获取空间中图片大小使用情况
     *
     * @param spaceSizeAnalyzeRequest 获取空间图片大小情况参数
     * @param request                 请求
     * @return
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSize(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyze = spaceAnalyzeApplicationService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceSizeAnalyze);
    }

    /**
     * 获取空间用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest 获取空间图片大小情况参数
     * @param request                 请求
     * @return
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> resultList = spaceAnalyzeApplicationService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(resultList);
    }

    /**
     * 获取空间排名
     *
     * @param spaceRankAnalyzeRequest 空间排名分析
     * @param request                 请求
     * @return
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<Space> spaceRank = spaceAnalyzeApplicationService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceRank);
    }

}
