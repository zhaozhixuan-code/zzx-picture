package com.zzx.zzxpicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量修改图片信息
 */
@Data
public class PictureEditByBatchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片id列表
     */
    private List<Long> pictureIdList;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 命名规则
     */
    private String nameRule;

}
