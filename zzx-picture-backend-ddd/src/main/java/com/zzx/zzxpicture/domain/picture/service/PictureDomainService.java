package com.zzx.zzxpicture.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzx.zzxpicture.domain.picture.entity.Picture;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zzx.zzxpicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.zzx.zzxpicture.interfaces.dto.picture.*;
import com.zzx.zzxpicture.interfaces.vo.picture.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 28299
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-09-13 17:08:54
 */
public interface PictureDomainService  {


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
     * 修改图片信息 （普通用户)
     *
     * @param pictureUpdateRequest
     * @param loginUser
     * @return
     */
    Boolean editPicture(PictureEditRequest pictureUpdateRequest, User loginUser);



    /**
     * 分页查询条件
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);



    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     * @return
     */
    Boolean doPictureReview(PictureReviewRequest pictureReviewRequest,  User loginUser);

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

}
