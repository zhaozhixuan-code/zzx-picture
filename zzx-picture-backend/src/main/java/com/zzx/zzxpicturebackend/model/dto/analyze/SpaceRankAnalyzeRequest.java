package com.zzx.zzxpicturebackend.model.dto.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间排名分析请求
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 获取前 N 个空间
     */
    private Integer topN = 10;
}
