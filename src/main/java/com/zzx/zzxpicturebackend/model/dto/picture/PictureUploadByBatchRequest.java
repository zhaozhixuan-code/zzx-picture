package com.zzx.zzxpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量上传图片请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 搜索图片的文本
     */
    private String searchText;

    /**
     * 要抓取图片的数量
     */
    private Integer count;


    /**
     * 名称的前缀
     */
    private String namePrefix;


}
