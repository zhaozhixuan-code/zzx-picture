package com.zzx.zzxpicture.interfaces.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户上传行为分析响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUserAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间区间
     */
    private String period;

    /**
     * 用户上传图片数量
     */
    private Long count;
}
