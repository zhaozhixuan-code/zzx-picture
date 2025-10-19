package com.zzx.zzxpicture.infrastructure.api.imagesearch.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.zzx.zzxpicture.infrastructure.exception.BusinessException;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取图片页面地址（step1)
 */
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        // 2. 发送 POST 请求到百度接口
        HttpResponse response = HttpRequest.post(url)
                // 这里需要指定acs-token 不然会响应系统异常
                .header("acs-token", RandomUtil.randomString(1))
                .form(formData)
                .timeout(5000)
                .execute();
        // 判断响应状态
        if (HttpStatus.HTTP_OK != response.getStatus()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
        }
        // 解析响应
        String responseBody = response.body();
        // System.out.println(responseBody);
        Map<String, Object> result = JSONUtil.toBean(responseBody, Map.class);

        // 3. 处理响应结果
        if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
        }
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        String rawUrl = (String) data.get("url");
        // 对 URL 进行解码
        String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
        // 如果 URL 为空
        if (searchResultUrl == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
        }
        return searchResultUrl;

    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://zzx-picture-1349365238.cos.ap-beijing.myqcloud.com//public/1965419228446646274/2025-09-18_0grGg6wu5GvtvHHD.jpg";
        String result = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果 URL：" + result);
    }
}
