package com.zzx.zzxpicture.application.service;


import com.zzx.zzxpicture.interfaces.dto.analyze.*;
import com.zzx.zzxpicture.interfaces.vo.space.analyze.*;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.domain.user.entity.User;

import java.util.List;

public interface SpaceAnalyzeApplicationService  {

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

    /**
     * 获取空间标签使用
     *
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 空间图片大小分析
     *
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取空间用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间排名
     *
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
