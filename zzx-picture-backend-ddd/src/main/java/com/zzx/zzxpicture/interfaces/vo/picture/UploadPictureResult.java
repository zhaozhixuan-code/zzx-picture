package com.zzx.zzxpicture.interfaces.vo.picture;

import lombok.Data;

/**
 * 上传图片返回结果
 */
@Data
public class UploadPictureResult {

    /**
     * 图片地址（webp）
     */
    private String url;

    /**
     * 缩略图片 url
     */
    private String thumbnailUrl;

    /**
     * 图片原图（用户上传的原图，没有被压缩）
     */
    private String originalUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private int picWidth;

    /**
     * 图片高度
     */
    private int picHeight;

    /**
     * 图片宽高比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间 id
     */
    private Long spaceId;

}
