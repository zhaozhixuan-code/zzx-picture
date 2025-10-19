package com.zzx.zzxpicture.application.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzx.zzxpicture.application.service.SpaceAnalyzeApplicationService;
import com.zzx.zzxpicture.infrastructure.common.SpaceAnalyzeRequest;
import com.zzx.zzxpicture.infrastructure.exception.BusinessException;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.interfaces.dto.analyze.*;
import com.zzx.zzxpicture.domain.picture.entity.Picture;
import com.zzx.zzxpicture.interfaces.vo.space.analyze.*;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.application.service.PictureApplicationService;
import com.zzx.zzxpicture.application.service.SpaceApplicationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 空间分析
 */
@Service
public class SpaceAnalyzeApplicationServiceImpl implements SpaceAnalyzeApplicationService {

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private PictureApplicationService pictureApplicationService;


    /**
     * 获取空间使用情况
     *
     * @param spaceUsageAnalyzeRequest 空间分析参数，查询某个空间
     * @param loginUser                登录用户
     * @return 空间使用情况
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceUsageAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceUsageAnalyzeRequest.isQueryAll();
        // 如果是获取全部空间或者查询公共空间
        if (queryAll || queryPublic) {
            // 检查权限,只能是管理员可访问
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            // 只查询图片的大小
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            // 填充查询条件
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            queryWrapper.select("picSize");
            List<Long> picSizeList = pictureApplicationService.getBaseMapper().selectMaps(queryWrapper).stream()
                    .map(map -> (Long) map.get("picSize")).collect(Collectors.toList());
            // 设置返回值
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(picSizeList.stream().mapToLong(Long::longValue).sum());
            spaceUsageAnalyzeResponse.setUsedCount((long) picSizeList.size());
            return spaceUsageAnalyzeResponse;
        } else {
            // 查询个人空间
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 检查权限
            spaceApplicationService.checkSpaceAuth(loginUser, space);
            // 封装返回值
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            // 设置空间大小使用比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            // 设置空间照片使用比例
            double countUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 获取空间中图片分类使用情况
     *
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        // 判断登录
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 查询数据 select category,count(id),sum(picSize) from picture group by category;
        // List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeList = pictureService.getCategoryStats();
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 填充查询条件
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        // 分组查询
        queryWrapper.select("category AS category",
                        "COUNT(*) AS count",
                        "SUM(picSize) AS totalSize")
                .groupBy("category");
        // 查询
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeList = pictureApplicationService.getBaseMapper().selectMaps(queryWrapper).stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    long count = ((Number) result.get("count")).longValue();
                    long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                }).collect(Collectors.toList());
        return spaceCategoryAnalyzeList;
    }


    /**
     * 获取空间标签使用
     *
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        // 判断登录
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 查询数据
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 填充查询条件
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags");
        // ["["java"]","["python","java"]"]
        List<String> tagJSONList = pictureApplicationService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        /* ArrayList<String> tagList = new ArrayList<>();
        for (String JsonTagStr : tagJSONList) {
            List<String> list = JSONUtil.toList(JsonTagStr, String.class);
            tagList.addAll(list);
        }
        HashMap<String, Long> tagMap = new HashMap<>();
        for (String tag : tagList) {
            if(!tagMap.containsKey(tag)){
                tagMap.put(tag, 1L);
            } else {
                tagMap.put(tag, tagMap.get(tag) + 1);
            }
        }
        // 降序排序
        List<SpaceTagAnalyzeResponse> result = tagMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()); */

        // 使用 flatMap 和 groupingBy 简化标签统计过程
        Map<String, Long> tagMap = tagJSONList.stream()
                .flatMap(jsonTagStr -> JSONUtil.toList(jsonTagStr, String.class).stream())
                .collect(Collectors.groupingBy(
                        tag -> tag,
                        Collectors.counting()
                ));

        // 转换为 SpaceTagAnalyzeResponse 列表并按数量降序排列
        List<SpaceTagAnalyzeResponse> result = tagMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 空间图片大小分析
     *
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 查询数据库
        List<Long> pictureSizeList = pictureApplicationService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest);
        Map<String, Long> sizeRange = new LinkedHashMap<>();
        sizeRange.put("0-1M", 0L);
        sizeRange.put("1-5M", 0L);
        sizeRange.put("5-10M", 0L);
        sizeRange.put("10-20M", 0L);
        for (Long pictureSize : pictureSizeList) {
            if (pictureSize < 1024 * 1024) {
                sizeRange.put("0-1M", sizeRange.get("0-1M") + 1);
            } else if (pictureSize < 1024 * 1024 * 5) {
                sizeRange.put("1-5M", sizeRange.get("1-5M") + 1);
            } else if (pictureSize < 1024 * 1024 * 10) {
                sizeRange.put("5-10M", sizeRange.get("5-10M") + 1);
            } else {
                sizeRange.put("10-20M", sizeRange.get("10-20M") + 1);
            }
        }
        List<SpaceSizeAnalyzeResponse> result = sizeRange.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * 获取空间用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        // 分析维度
        // SELECT DATE_FORMAT(createTime,'%Y-%m') AS period, COUNT(*) AS count FROM picture GROUP BY period ORDER BY period;
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持时间维度");
        }
        queryWrapper.groupBy("period").orderByAsc("period");
        // 查询数据库
        // [{"period": "2024-01-01","count": 15},{"period": "2024-01-02","count": 8}]
        List<Map<String, Object>> queryResult = pictureApplicationService.getBaseMapper().selectMaps(queryWrapper);

        List<SpaceUserAnalyzeResponse> result = queryResult.stream()
                .map(map -> new SpaceUserAnalyzeResponse(map.get("period").toString(), Long.parseLong(map.get("count").toString())))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * 获取空间排名
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 校验权限,仅管理员可查看空间排名
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "totalSize", "totalCount")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());
        return spaceApplicationService.list(queryWrapper);
    }



    /**
     * 检查空间分析权限
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        // 如果为查询全部空间或者查询公共空间
        if (queryAll || queryPublic) {
            // 只能是管理员可访问
            ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        } else {
            // 查询个人空间,只能是管理员或者本人进行访问
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceApplicationService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 填充查询条件
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        // 分析全空间
        if (queryAll) {
            return;
        } else if (queryPublic) {
            // 分析公共空间
            queryWrapper.isNull("spaceId");
            return;
        } else if (spaceId != null) {
            // 分析个人空间
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new RuntimeException("空间分析参数错误");
    }
}
