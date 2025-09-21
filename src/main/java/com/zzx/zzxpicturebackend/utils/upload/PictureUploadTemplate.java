package com.zzx.zzxpicturebackend.utils.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.*;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.zzx.zzxpicturebackend.config.CosClientConfig;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.model.vo.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
 * 模板方法，定义图片上传的标准流程
 *
 * @param inputSource      文件源（可以是本地文件路径、URL或文件对象）
 * @param uploadPathPrefix 上传路径前缀，用于构建完整的上传路径
 * @return UploadPictureResult 上传结果封装对象
 */
public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
    // 1. 图片格式和大小校验
    check(inputSource);

    // 2. 构建上传路径和文件名
    // 生成16位随机字符串作为文件名的一部分，避免文件名冲突
    String uuid = RandomUtil.randomString(16);
    String originalFilename = getOriginalFilename(inputSource);
    // 构建完整上传路径：/路径前缀/日期_uuid_原文件名
    String uploadPath = String.format("/%s/%s_%s", uploadPathPrefix, DateUtil.formatDate(new Date()), uuid + "_" + originalFilename);

    // 3. 处理并上传图片文件
    File file = null;
    try {
        // 创建临时文件用于上传操作
        file = File.createTempFile(uploadPath, null);
        processFile(inputSource, file);

        // 4. 构建上传请求并设置图片处理参数
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), uploadPath, file);

        // 配置图片处理操作
        PicOperations picOperations = new PicOperations();
        // 设置为1表示返回原图信息
        picOperations.setIsPicInfo(1);

        // 4.1 配置WebP格式压缩规则
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 生成WebP格式文件名
        String webpKey = FileUtil.mainName(uploadPath) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        // 设置图片处理规则：转换为WebP格式
        compressRule.setRule("imageMogr2/format/webp");
        // 设置目标存储桶
        compressRule.setBucket(cosClientConfig.getBucket());
        // 设置处理后文件的存储路径
        compressRule.setFileId(webpKey);
        rules.add(compressRule);

        // 应用图片处理规则
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);

        // 5. 执行上传操作
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

        // 6. 解析上传结果和处理结果
        // 获取原图信息
        ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
        // 获取图片处理结果
        ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
        List<CIObject> objectList = processResults.getObjectList();

        // 如果存在处理后的图片（如压缩后的WebP），优先返回处理后的结果
        if (CollUtil.isNotEmpty(objectList)) {
            CIObject compressedCiObject = objectList.get(0);
            // 封装压缩图返回结果
            return getUploadPictureResult(originalFilename, compressedCiObject);
        }

        // 7. 如果没有处理结果，则返回原图信息
        return getUploadPictureResult(imageInfo, originalFilename, file, uploadPath);

    } catch (Exception e) {
        log.error("上传图片失败，文件源：{}，上传路径：{}", inputSource, uploadPath, e);
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
    } finally {
        // 8. 清理临时文件资源
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
     * 封装返回结果 （压缩后的对象）
     *
     * @param originalFilename   原文件名
     * @param compressedCiObject 压缩后的对象
     * @return
     */
    private UploadPictureResult getUploadPictureResult(String originalFilename, CIObject compressedCiObject) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
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
