package com.zzx.zzxpicturebackend.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.zzxpicturebackend.model.dto.analyze.SpaceCategoryAnalyzeRequest;
import com.zzx.zzxpicturebackend.model.dto.analyze.SpaceUsageAnalyzeRequest;
import com.zzx.zzxpicturebackend.model.po.Space;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.analyze.SpaceCategoryAnalyzeResponse;
import com.zzx.zzxpicturebackend.model.vo.analyze.SpaceUsageAnalyzeResponse;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间使用情况
     *
     * @param spaceUsageAnalyzeRequest 空间分析参数，查询某个空间
     * @param loginUser                登录用户
     * @return 空间使用情况
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 获取空间中图片分类使用情况
     *
     * @param spaceCategoryAnalyzeRequest 获取空间图片分类分析结果参数
     * @param loginUser                   登录用户
     * @return 图片分类结果
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);
}
