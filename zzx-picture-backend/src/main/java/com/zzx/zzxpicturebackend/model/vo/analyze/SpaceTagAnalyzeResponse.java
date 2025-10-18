package com.zzx.zzxpicturebackend.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间标签分析响应类
 * 返回给前端为列表形式，一个标签有多少张图片
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签
     */
    private String tag;

    /**
     * 该标签的照片数量
     */
    private Long count;
}
