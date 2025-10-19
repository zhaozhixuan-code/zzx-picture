package com.zzx.zzxpicture.interfaces.dto.picture;

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

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}

