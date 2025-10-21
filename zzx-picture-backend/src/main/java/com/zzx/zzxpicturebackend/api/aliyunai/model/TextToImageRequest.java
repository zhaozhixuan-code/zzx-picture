package com.zzx.zzxpicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;

/**
 * 阿里云文生图请求类
 * 同步调用
 */
@Data
public class TextToImageRequest {

    /**
     * 模型，例如 "qwen-image-plus"
     */
    private String model = "qwen-image-plus";

    /**
     * 输入参数
     */
    private Input input;

    private Parameters parameters;

    /**
     * 输入参数类
     */
    @Data
    public static class Input {
        private List<Message> messages;

        // 便捷方法：添加消息
        public void addMessage(Message message) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(message);
        }
    }

    /**
     * 消息类
     */
    @Data
    public static class Message {

        /**
         * 消息的角色。此参数必须设置为user
         */
        private String role = "user";

        /**
         * 消息的内容，包括图像与提示词，content只能包含一个text
         */
        private List<Content> content;

        // 便捷方法：添加内容
        public void addContent(Content content) {
            if (this.content == null) {
                this.content = new ArrayList<>();
            }
            this.content.add(content);
        }

        // 便捷方法：直接添加文本内容
        public void addTextContent(String text) {
            Content content = new Content();
            content.setText(text);
            addContent(content);
        }
    }

    /**
     * 内容类
     */
    @Data
    public static class Content {

        /**
         * 正向提示词，用来描述图像中期望包含的元素和视觉特点
         */
        private String text;
    }

    /**
     * 参数配置类（可选）
     */
    @Data
    public static class Parameters {

        /**
         * 反向提示词，用来描述图像中不希望包含的元素和视觉特点
         */
        @Alias("negative_prompt")
        private String negativePrompt;

        /**
         * 是否开启prompt智能改写，开启后会使用大模型对输入的prompt进行改写，仅对正向提示词有效
         * 对于焦段的输入有明显提升，但会增加3-4秒耗时
         * ture 为开启，false 为关闭
         */
        @Alias("prompt_extend")
        private Boolean promptExtend = true;

        /**
         * 是否添加水印,默认为false
         */
        private Boolean watermark = false;

        /**
         * 输出图像的分辨率，格式为宽*高。默认分辨率为1328*1328。
         */
        private String size;

        /**
         * 生成图像的数量，默认为1
         */
        private Integer n;

        /**
         * 随机数种子，用于生成相同的结果
         */
        private Long seed;
    }

    // ========== 便捷构建方法 ==========

    /**
     * 创建请求的便捷方法
     */
    public static TextToImageRequest create(String prompt) {
        return create(prompt, "qwen-image-plus");
    }

    public static TextToImageRequest create(String prompt, String model) {
        TextToImageRequest request = new TextToImageRequest();
        request.setModel(model);

        // 创建内容
        Content content = new Content();
        content.setText(prompt);

        // 创建消息
        Message message = new Message();
        message.setRole("user");
        message.setContent(List.of(content)); // 使用List.of创建不可变列表

        // 创建输入
        Input input = new Input();
        input.setMessages(List.of(message));

        request.setInput(input);

        // 设置默认参数
        Parameters parameters = new Parameters();
        parameters.setPromptExtend(true);
        parameters.setWatermark(false);
        parameters.setSize("1328*1328");
        parameters.setN(1);
        request.setParameters(parameters);

        return request;
    }

    /**
     * 使用构建器模式创建请求
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器类
     */
    public static class Builder {
        private String model = "qwen-image-plus";
        private String prompt;
        private Parameters parameters = new Parameters();

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder negativePrompt(String negativePrompt) {
            this.parameters.setNegativePrompt(negativePrompt);
            return this;
        }

        public Builder promptExtend(Boolean promptExtend) {
            this.parameters.setPromptExtend(promptExtend);
            return this;
        }

        public Builder watermark(Boolean watermark) {
            this.parameters.setWatermark(watermark);
            return this;
        }

        public Builder size(String size) {
            this.parameters.setSize(size);
            return this;
        }

        public Builder n(Integer n) {
            this.parameters.setN(n);
            return this;
        }

        public Builder seed(Long seed) {
            this.parameters.setSeed(seed);
            return this;
        }

        public TextToImageRequest build() {
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new IllegalArgumentException("提示词不能为空");
            }

            return TextToImageRequest.create(prompt, model);
        }
    }
}