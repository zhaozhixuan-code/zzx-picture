package com.zzx.zzxpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.zzxpicturebackend.model.dto.picture.PictureQueryRequest;
import com.zzx.zzxpicturebackend.model.dto.picture.PictureReviewRequest;
import com.zzx.zzxpicturebackend.model.dto.picture.PictureUpdateRequest;
import com.zzx.zzxpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zzx.zzxpicturebackend.model.po.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 28299
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-09-13 17:08:54
 */
public interface PictureService extends IService<Picture> {


    /**
     * 上传图片
     *
     * @param file
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile file, PictureUploadRequest pictureUploadRequest, User loginUser);


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
    Boolean editPicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request);

    /**
     * 根据id获取图片VO
     *
     * @param id
     * @return
     */
    PictureVO getPictureVOById(Long id);


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
     * @param pictureList
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> pictureList);


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
}
