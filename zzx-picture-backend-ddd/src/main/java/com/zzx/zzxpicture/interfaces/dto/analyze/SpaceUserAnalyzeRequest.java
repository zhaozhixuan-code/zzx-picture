package com.zzx.zzxpicture.interfaces.dto.analyze;

import com.zzx.zzxpicture.infrastructure.common.SpaceAnalyzeRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户上传行为分析请求
 * 支持分析某个用户上传图片的情况
 * 根据时间维度（日、周、月）进行查询，即每个月的上传情况
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    /**
     * 用户id（可选）
     */
    private Long userId;

    /**
     * 分析的时间维度：day / week / month
     */
    private String timeDimension;
}
