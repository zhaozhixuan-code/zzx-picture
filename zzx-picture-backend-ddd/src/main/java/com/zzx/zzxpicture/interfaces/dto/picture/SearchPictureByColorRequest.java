package com.zzx.zzxpicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 颜色搜图请求
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间id
     */
    private Long spaceId;

}
