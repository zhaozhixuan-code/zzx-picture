package com.zzx.zzxpicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片审核请求
 */
@Data
public class PictureReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片id
     */
    private Long id;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;


}
