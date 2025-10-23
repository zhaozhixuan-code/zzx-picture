package com.zzx.zzxpicturebackend.api.aliyunai;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.zzx.zzxpicturebackend.api.aliyunai.model.*;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 阿里云相关接口
 * 目前包含 AI 扩图、文生图
 */
@Slf4j
@Component
public class AliyunAiApi {

    @Value("${aliyun.api-key}")
    private String apiKey;

    @Value("${aliyun.image-model}")
    private String model;

    // 文生图模型
    @Value("${aliyun.text-to-image-model}")
    private String textToImageModel;


    // 创建 AI 扩图任务地址
    private static final String CREATE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询 AI 扩图任务地址
    private static final String GET_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    // 文生图接口地址
    private static final String TEXT_TO_IMAGE_URL = "https://dashscope.aliyuncs.com/api/v1";


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
                log.error("AI扩图失败：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图失败：" + httpResponse.body());
            }
            GetOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
            log.info("AI 扩图结果信息：{}", response);
            String errCode = response.getOutput().getCode();
            if (StrUtil.isNotBlank(errCode)) {
                String message = response.getOutput().getMessage();
                log.info("AI扩图失败：{}", message);
                if ("InvalidParameter.ImageResolution".equals(response.getOutput().getCode())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片不能低于512×512像素且不超过4096×4096像素");
                }
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图失败：" + message);
            }
            return response;
        }
    }

    /**
     * 文生图
     *
     * @param request
     * @return
     */
    public TextToImageResponse getTextToImage(TextToImageRequest request) {
        // 校验参数
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 获取用户输入
        String message = request.getInput().getMessages().get(0).getContent().get(0).getText();
        // 构造请求参数
        MultiModalConversation conversation = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("text", message)
                )).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(textToImageModel)                          // 设置文生图模型
                .messages(Collections.singletonList(userMessage)) // 设置用户输入
                .build();
        // 发起请求
        MultiModalConversationResult result = null;
        try {
            result = conversation.call(param);
            // log.info("response:{}", result);
        } catch (NoApiKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "apiKey错误");
        } catch (UploadFileException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "阿里云服务器上传文件失败");
        }
        TextToImageResponse response = BeanUtil.toBean(result, TextToImageResponse.class);
        // 处理响应结果
        String errorCode = response.getCode();
        if (StrUtil.isNotBlank(errorCode)) {
            String errorMessage = response.getMessage();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "错误代码为：" + errorCode + "错误信息为：" + errorMessage);
        }
        return response;
    }

}
