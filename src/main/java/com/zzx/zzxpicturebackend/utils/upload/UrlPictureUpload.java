package com.zzx.zzxpicturebackend.utils.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;


/**
 * 上传 url 地址的图片
 */
@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate {

    /**
     * 检查源文件地址
     *
     * @param inputSource
     */
    @Override
    protected void check(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 验证URL是否为空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "图片URL不能为空");

        // 验证URL格式是否正确
        ThrowUtils.throwIf(!ReUtil.isMatch("^https?://.*", fileUrl), ErrorCode.PARAMS_ERROR, "图片URL格式错误");


        // 使用HEAD请求检查文件大小
        HttpResponse response = null;
        try {
             response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();

            // 检查响应状态码
            ThrowUtils.throwIf(response.getStatus() != HttpStatus.HTTP_OK, ErrorCode.PARAMS_ERROR, "无法访问图片URL");

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
     * 获取源文件的原始文件名
     *
     * @param inputSource
     * @return
     */
    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);// 这个方法是从 url 中返回文件名
    }

    /**
     * 处理输入源并且生成本地临时文件
     *
     * @param inputSource
     * @param file
     */
    @Override
    protected void processFile(Object inputSource, File file) {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }
}
