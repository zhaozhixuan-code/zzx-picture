package com.zzx.zzxpicture.infrastructure.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.*;
import cn.hutool.http.HttpUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zzx.zzxpicture.infrastructure.config.CosClientConfig;
import com.zzx.zzxpicture.infrastructure.exception.BusinessException;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.interfaces.vo.picture.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

@Component
@Slf4j
// 此方法废弃 移动至 upload 文件夹中的方法
@Deprecated
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
     * 通过URL上传图片
     *
     * @param fileUrl
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        // 校验图片的URL
        // 1. 图片校验
        check(fileUrl);
        // 2. 图片上传地址
        // 获取文件名：uuid.jpg
        String uuid = RandomUtil.randomString(16);
        String originalFilename = FileUtil.getName(fileUrl);
        // 获取文件后缀
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        // 获取上传地址 /public/userId/2025.9.13_uuid.jpg
        String uploadPath = String.format("/%s/%s_%s.%s", uploadPathPrefix, DateUtil.formatDate(new Date()), uuid, extension);
        // 3. 上传照片
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // multipartFile.transferTo(file);
            file = File.createTempFile(uploadPath, null);
            HttpUtil.downloadFile(fileUrl, file);
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
     * @param fileUrl 文件URL
     */
    private void check(String fileUrl) {
        // 验证URL是否为空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "图片URL不能为空");

        // 验证URL格式是否正确
        ThrowUtils.throwIf(!ReUtil.isMatch("^https?://.*", fileUrl), ErrorCode.PARAMS_ERROR, "图片URL格式错误");


        // 使用HEAD请求检查文件大小
        cn.hutool.http.HttpResponse response = null;
        try {
            cn.hutool.http.HttpRequest request = cn.hutool.http.HttpRequest.head(fileUrl);
            response = request.execute();

            // 检查响应状态码
            ThrowUtils.throwIf(response.getStatus() != 200, ErrorCode.PARAMS_ERROR, "无法访问图片URL");

            // 获取文件大小
            long maxSize = 1024 * 1024 * 20; // 20MB
            String contentLengthStr = response.header("Content-Length");

            if (contentLengthStr != null && !contentLengthStr.isEmpty()) {
                long fileSize = Long.parseLong(contentLengthStr);
                ThrowUtils.throwIf(fileSize > maxSize, ErrorCode.PARAMS_ERROR, "图片大小不能超过20MB");
            }

            // 检查Content-Type
            String contentType = response.header("Content-Type");
            if (contentType != null && !contentType.isEmpty()) {
                boolean isValidImageType = contentType.startsWith("image/jpeg") ||
                        contentType.startsWith("image/png") ||
                        contentType.startsWith("image/jpg");
                ThrowUtils.throwIf(!isValidImageType, ErrorCode.PARAMS_ERROR, "URL不是有效的图片资源");
            }
        } catch (NumberFormatException e) {
            log.error("解析文件大小失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法获取图片大小信息");
        } catch (Exception e) {
            log.error("HEAD请求检查文件失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法验证图片URL");
        } finally {
            // 释放HttpResponse资源
            if (response != null) {
                response.close();
            }
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
        // 校验图片大小不超过20MB
        long MaxSize = 1024 * 1024 * 20;
        ThrowUtils.throwIf(file.getSize() > MaxSize, ErrorCode.PARAMS_ERROR, "图片大小不能超过20MB");
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
