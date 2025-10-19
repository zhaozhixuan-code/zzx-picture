package com.zzx.zzxpicture.infrastructure.manager.upload;

import cn.hutool.core.util.ArrayUtil;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;


/**
 * 本地文件上传
 */
@Service
@Slf4j
public class FilePictureUpload extends PictureUploadTemplate {

    /**
     * 检查图片
     *
     * @param inputSource
     */
    @Override
    protected void check(Object inputSource) {
        MultipartFile file = (MultipartFile) inputSource;
        // 判断图片是否为空
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR, "图片不能为空");
        // 校验图片大小不超过20MB
        long MaxSize = 1024 * 1024 * 20;
        ThrowUtils.throwIf(file.getSize() > MaxSize, ErrorCode.PARAMS_ERROR, "图片大小不能超过20MB");
        // 校验图片格式，只允许jpg、png、jpeg、raw格式
        String[] allowedExtensions = {"jpg", "jpeg", "png", "bmp", "webp", "raw"};
        String fileName = file.getOriginalFilename();
        // 获取文件后缀
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        // 判断文件后缀是否在允许的列表中
        ThrowUtils.throwIf(!ArrayUtil.contains(allowedExtensions, extension),
                ErrorCode.PARAMS_ERROR, "图片格式错误");
    }

    /**
     * 获取原始文件名
     *
     * @return
     */
    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile file = (MultipartFile) inputSource;
        return file.getOriginalFilename();
    }

    /**
     * 处理输入源并且生成本地临时文件
     *
     * @param inputSource
     * @param file
     */
    @Override
    protected void processFile(Object inputSource, File file) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        try {
            multipartFile.transferTo(file);
        } catch (Exception e) {
            log.error("文件处理错误", e);
        }
    }
}
