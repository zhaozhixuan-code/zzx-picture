package com.zzx.zzxpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片上传请求
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    /**
     * 图片 url
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String PicName;

    /**
     * 分类
     */
    private String category;

    /**
     *
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}

