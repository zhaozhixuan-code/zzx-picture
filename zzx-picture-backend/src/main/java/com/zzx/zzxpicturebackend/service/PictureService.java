package com.zzx.zzxpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.zzxpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zzx.zzxpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.zzx.zzxpicturebackend.api.aliyunai.model.TextToImageRequest;
import com.zzx.zzxpicturebackend.api.aliyunai.model.TextToImageResponse;
import com.zzx.zzxpicturebackend.model.dto.analyze.SpaceSizeAnalyzeRequest;
import com.zzx.zzxpicturebackend.model.dto.picture.*;
import com.zzx.zzxpicturebackend.model.po.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.PictureVO;
import com.zzx.zzxpicturebackend.model.vo.analyze.SpaceCategoryAnalyzeResponse;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 28299
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-09-13 17:08:54
 */
public interface PictureService extends IService<Picture> {


    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);


    /**
     * 删除图片
     *
     * @param id
     * @param request
     * @return
     */
    Boolean deletePicture(Long id, HttpServletRequest request);

    /**
     * 修改图片信息（管理员）
     *
     * @param pictureUpdateRequest
     * @return
     */
    Boolean updatePicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request);


    /**
     * 修改图片信息 （普通用户)
     *
     * @param pictureUpdateRequest
     * @param request
     * @return
     */
    Boolean editPicture(PictureEditRequest pictureUpdateRequest, HttpServletRequest request);

    /**
     * 根据id获取图片VO
     *
     * @param id
     * @param request
     * @return
     */
    PictureVO getPictureVOById(Long id, HttpServletRequest request);


    /**
     * 分页查询条件
     *
     * @param pictureQueryRequest
     * @return
     */
    Wrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 分页查询VO
     *
     * @param current
     * @param size
     * @param pictureQueryRequest
     * @return
     */
    Page<PictureVO> getPictureVOPage(Long current, Long size, PictureQueryRequest pictureQueryRequest, HttpServletRequest request);


    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    Boolean doPictureReview(PictureReviewRequest pictureReviewRequest, HttpServletRequest request);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fileReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功的图片数量
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 删除图片文件
     *
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);


    /**
     * 根据颜色搜索图片
     *
     * @param searchPictureByColorRequest
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(SearchPictureByColorRequest searchPictureByColorRequest, User loginUser);

    /**
     * 批量修改图片信息
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建 AI 扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);


    /**
     * 获取 AI 扩图任务结果
     *
     * @param taskId
     * @return
     */
    GetOutPaintingTaskResponse getPictureOutPaintingTask(String taskId);


    TextToImageResponse getTextToImage(TextToImageRequest request);

    /**
     * 获取图片分类统计
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getCategoryStats();

    /**
     * 获取空间图片大小
     *
     * @param spaceSizeAnalyzeRequest
     * @return
     */
    List<Long> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest);

}
