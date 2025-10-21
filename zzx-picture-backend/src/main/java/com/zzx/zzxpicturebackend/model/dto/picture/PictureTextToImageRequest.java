package com.zzx.zzxpicturebackend.model.dto.picture;


import com.zzx.zzxpicturebackend.api.aliyunai.model.TextToImageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 文生图片请求
 */
@Data
public class PictureTextToImageRequest implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 用户输入
     */
    private String userPrompt;

}
