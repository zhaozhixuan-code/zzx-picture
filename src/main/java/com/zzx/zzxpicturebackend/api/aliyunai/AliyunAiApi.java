package com.zzx.zzxpicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.zzx.zzxpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.zzx.zzxpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zzx.zzxpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 阿里云图像扩图接口
 */
@Slf4j
@Component
public class AliyunAiApi {

    @Value("${aliyun.api-key}")
    private String apiKey;

    @Value("${aliyun.image-model}")
    private String model;


    // 创建任务地址
    private static final String CREATE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务地址
    private static final String GET_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";


    /**
     * 创建图像扩图任务
     *
     * @param request
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest request) {
        // 参数校验
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 设置模型
        request.setModel(model);
        // 构造请求参数
        HttpRequest httpRequest = HttpRequest.post(CREATE_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(request));
        // 发送请求
        try (HttpResponse httpResponse = httpRequest.execute()) {
            // 处理失败情况
            if (!httpResponse.isOk()) {
                log.error("创建图像扩图任务失败:{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图失败:" + httpResponse.body());
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String code = response.getCode();
            if (StrUtil.isNotBlank(code)) {
                String message = response.getMessage();
                log.info("AI 扩图失败：{}", message);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图失败:" + message);
            }
            return response;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        // curl -X GET https://dashscope.aliyuncs.com/api/v1/tasks/86ecf553-d340-4e21-xxxxxxxxx \
        // --header "Authorization: Bearer $DASHSCOPE_API_KEY"
        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        // 构造请求参数
        HttpRequest httpRequest = HttpRequest.get(String.format(GET_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey);
        // 发送请求
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("查询任务失败:{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询任务失败:" + httpResponse.body());
            }
            GetOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
            return response;
        }
    }
}
