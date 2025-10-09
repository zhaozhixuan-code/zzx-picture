package com.zzx.zzxpicturebackend.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.zzxpicturebackend.model.dto.analyze.SpaceUsageAnalyzeRequest;
import com.zzx.zzxpicturebackend.model.po.Space;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.analyze.SpaceUsageAnalyzeResponse;

public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间使用情况
     *
     * @param spaceUsageAnalyzeRequest 空间分析参数，查询某个空间
     * @param loginUser                登录用户
     * @return 空间使用情况
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

}
