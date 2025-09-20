package com.zzx.zzxpicturebackend.utils.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.*;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zzx.zzxpicturebackend.config.CosClientConfig;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.model.vo.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

/**
 * 两种上传图片模板
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * 模版方法，定义上传流程
     *
     * @param inputSource      文件源
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 图片校验
        check(inputSource);
        // 2. 图片上传地址
        // 文件名：uuid.jpg
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        // 获取上传地址 /public/userId/2025.9.13_uuid.jpg
        String uploadPath = String.format("/%s/%s_%s", uploadPathPrefix, DateUtil.formatDate(new Date()), uuid + "_" + originalFilename);
        // 3. 上传照片
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            processFile(inputSource, file);
            // 4. 上传文件到cos
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), uploadPath, file);
            // 图片处理
            PicOperations picOperations = new PicOperations();
            // 1 表示返回原图信息
            picOperations.setIsPicInfo(1);
            // 构造处理参数
            putObjectRequest.setPicOperations(picOperations);
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 5. 封装返回结果
            return getUploadPictureResult(imageInfo, originalFilename, file, uploadPath);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            // 清理资源
            deleteTempFile(file);
        }
    }


    /**
     * 图片校验 (本地图片或者URL)
     *
     * @param inputSource
     */
    protected abstract void check(Object inputSource);


    /**
     * 获取输入源的原始文件名字
     *
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源并且生成本地临时文件
     *
     * @param inputSource
     * @param file
     */
    protected abstract void processFile(Object inputSource, File file);

    /**
     * 封装返回结果
     *
     * @param imageInfo
     * @param originalFilename
     * @param file
     * @param uploadPath
     * @return
     */
    private UploadPictureResult getUploadPictureResult(ImageInfo imageInfo, String originalFilename, File file, String uploadPath) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     *
     * @param file
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("删除临时文件失败");
        }
    }
}
