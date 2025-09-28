package com.zzx.zzxpicturebackend.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.zzxpicturebackend.annotation.AuthCheck;
import com.zzx.zzxpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.zzx.zzxpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zzx.zzxpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.zzx.zzxpicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.zzx.zzxpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.zzx.zzxpicturebackend.common.BaseResponse;
import com.zzx.zzxpicturebackend.common.ResultUtils;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import com.zzx.zzxpicturebackend.model.dto.picture.*;
import com.zzx.zzxpicturebackend.model.enums.PictureReviewEnum;
import com.zzx.zzxpicturebackend.model.enums.UserRoleEnum;
import com.zzx.zzxpicturebackend.model.po.Picture;
import com.zzx.zzxpicturebackend.model.po.PictureTagCategory;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.PictureVO;
import com.zzx.zzxpicturebackend.service.PictureService;
import com.zzx.zzxpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 照片控制器
 */
@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;


    /**
     * 通过本地文件上传照片
     *
     * @param file                 上传的照片
     * @param pictureUploadRequest 上传照片是否为修改
     * @param request              请求
     * @return 上传成功后的照片信息
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile file,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {

        // 获取当前用户
        User loginUser = userService.getLoginUser(request);
        // 上传照片，返回照片信息
        PictureVO pictureVO = pictureService.uploadPicture(file, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过 URL 上传照片
     *
     * @param pictureUploadRequest 上传照片是否为修改
     * @param request              请求
     * @return 上传成功后的照片信息
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {

        // 获取当前用户
        User loginUser = userService.getLoginUser(request);
        // 获取照片的 url
        String fileUrl = pictureUploadRequest.getFileUrl();
        // 上传照片，返回照片信息
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }


    /**
     * 删除照片
     *
     * @param id 照片id
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody Long id, HttpServletRequest request) {
        Boolean result = pictureService.deletePicture(id, request);
        return ResultUtils.success(result);
    }

    /**
     * 修改照片（管理员）
     *
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        Boolean result = pictureService.updatePicture(pictureUpdateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 修改照片（用户）
     *
     * @param pictureEditRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        Boolean result = pictureService.editPicture(pictureEditRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 获取照片信息 （管理员）
     *
     * @param id 照片id
     * @return 照片信息
     */
    @GetMapping("/get")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Picture> getPictureById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    /**
     * 获取照片信息 （用户）
     *
     * @param id 照片id
     * @return 照片信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        PictureVO pictureVO = pictureService.getPictureVOById(id, request);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 获取照片列表（管理员）
     *
     * @param pictureQueryRequest 照片查询参数
     * @return 照片列表
     */
    @PostMapping("/list/page")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        Page<Picture> pictureList = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pictureList);
    }

    /**
     * 获取照片列表
     *
     * @param pictureQueryRequest 照片查询参数
     * @return 照片列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 普通用户只能看到审核通过的照片 这里移入service中校验
        // pictureQueryRequest.setReviewStatus(PictureReviewEnum.PASS.getValue());
        // 分页查询
        Page<PictureVO> pictureVOList = pictureService.getPictureVOPage(current, size, pictureQueryRequest, request);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 获取预制的标签和分类
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        // 获取分类
        List<String> categoryList = Arrays.asList("风景", "动漫", "动物", "人像", "海报");
        // 获取标签
        List<String> tagList = Arrays.asList("热门", "城市", "高清", "艺术", "校园", "星空", "女孩", "时尚");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核照片（只有管理员才能审核）
     *
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        // 审核照片
        Boolean result = pictureService.doPictureReview(pictureReviewRequest, request);
        // 返回
        return ResultUtils.success(result);
    }

    /**
     * 批量上传照片
     * 从 bing 获取图片
     *
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch/bing")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Integer> uploadPictureByBatchWithBing(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        // 审核照片
        User loginUser = userService.getLoginUser(request);
        Integer count = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        // 返回
        return ResultUtils.success(count);
    }


    /**
     * 以图搜图
     *
     * @param searchPictureByPictureRequest
     * @return
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取图片的原始地址（未被压缩的）
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(picture.getOriginalUrl());
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 以颜色搜图
     *
     * @param searchPictureByColorRequest
     * @param request
     * @return
     */
    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        List<PictureVO> imageSearchResults = pictureService.searchPictureByColor(searchPictureByColorRequest, loginUser);
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 批量修改图片信息
     *
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/out_patting/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                                                             HttpServletRequest httpServletRequest) {
        // 校验参数
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null ||
                createPictureOutPaintingTaskRequest.getPictureId() == null, ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 创建 AI 扩图任务
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);

        return ResultUtils.success(response);
    }

    /**
     * 获取 AI 扩图任务
     *
     * @param taskId 任务id
     * @return
     */
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId){
        // 校验参数
        ThrowUtils.throwIf(taskId == null, ErrorCode.PARAMS_ERROR);

        GetOutPaintingTaskResponse response = pictureService.getPictureOutPaintingTask(taskId);
        return ResultUtils.success(response);
    }
}
