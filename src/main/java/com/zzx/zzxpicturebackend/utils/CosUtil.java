package com.zzx.zzxpicturebackend.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zzx.zzxpicturebackend.config.CosClientConfig;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import com.zzx.zzxpicturebackend.model.vo.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

@Component
@Slf4j
public class CosUtil {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * 上传对象
     *
     * @param key  对象键(Key)是对象在存储桶中的唯一标识。
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);

        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 对象键(Key)是对象在存储桶中的唯一标识。
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }


    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 1. 图片校验
        check(multipartFile);
        // 2. 图片上传地址
        // 文件名：uuid.jpg
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        // 获取文件后缀
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        // 获取上传地址 /public/userId/2025.9.13_uuid.jpg
        String uploadPath = String.format("/%s/%s_%s.%s", uploadPathPrefix, DateUtil.formatDate(new Date()), uuid, extension);
        // 3. 上传照片
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
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
        } catch (Exception e) {
            log.error("上传图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            // 清理资源
            deleteTempFile(file);
        }
    }


    /**
     * 图片校验
     *
     * @param file 文件
     */
    private void check(MultipartFile file) {
        // 判断图片是否为空
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR, "图片不能为空");
        // 校验图片大小不超过10MB
        long MaxSize = 1024 * 1024 * 10;
        ThrowUtils.throwIf(file.getSize() > MaxSize, ErrorCode.PARAMS_ERROR, "图片大小不能超过10MB");
        // 校验图片格式，只允许jpg、png、jpeg、raw格式
        String[] allowedExtensions = {"jpg", "png", "jpeg", "raw"};
        String fileName = file.getOriginalFilename();
        // 获取文件后缀
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        // 判断文件后缀是否在允许的列表中
        ThrowUtils.throwIf(!ArrayUtil.contains(allowedExtensions, extension),
                ErrorCode.PARAMS_ERROR, "图片格式错误");
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
