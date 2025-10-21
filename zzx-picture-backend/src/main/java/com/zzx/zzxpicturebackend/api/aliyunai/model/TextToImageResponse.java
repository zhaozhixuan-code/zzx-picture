package com.zzx.zzxpicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文生图响应类
 * 同步调用
 */
@Data
public class TextToImageResponse {

    /**
     * 请求ID
     */
    @Alias("request_id")
    private String requestId;

    /**
     * 输出结果
     */
    private Output output;

    /**
     * 使用情况
     */
    private Usage usage;

    /**
     * 错误代码
     */
    private String code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 输出结果类
     */
    @Data
    public static class Output {

        /**
         * 模型生成的内容，可以包含多个对象
         */
        private List<Choice> choices;

        /**
         * 任务指标
         */
        @Alias("task_metric")
        private TaskMetric taskMetric;
    }

    /**
     * 模型生成的内容
     */
    @Data
    public static class Choice {

        /**
         * 自然停止时输出为stop
         */
        @Alias("finish_reason")
        private String finishReason;

        /**
         * 输出的信息
         */
        private Message message;
    }

    /**
     * 消息类
     */
    @Data
    public static class Message {

        /**
         * 消息的角色，固定为assistant
         */
        private String role;

        /**
         * 输出内容
         */
        private List<Content> content;
    }

    /**
     * 内容类,为模型生成图片的URL地址
     */
    @Data
    public static class Content {

        /**
         * 图片的URL地址
         */
        private String image;
    }

    /**
     * 任务指标类
     */
    @Data
    public static class TaskMetric {

        /**
         * 总的任务数量
         */
        private Integer total;

        /**
         * 成功的任务数量
         */
        private Integer succeeded;

        /**
         * 失败的任务数量
         */
        private Integer failed;
    }

    /**
     * 输出信息统计，只对成功的结果计数
     */
    @Data
    public static class Usage {

        /**
         * 模型生成的图片数量，目前固定为 1
         */
        @Alias("image_count")
        private Integer imageCount;

        /**
         * 模型生成图片的宽度
         */
        private Integer width;

        /**
         * 模型生成图片的高度
         */
        private Integer height;
    }
}
