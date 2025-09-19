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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;


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
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        try {
            // 1. 验证 URL 格式
            new URL(fileUrl); // 验证是否是合法的 URL
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        // 2. 校验 URL 协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 3. 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
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
