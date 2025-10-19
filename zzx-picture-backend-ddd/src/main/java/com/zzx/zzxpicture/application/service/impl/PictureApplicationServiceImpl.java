package com.zzx.zzxpicture.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.zzx.zzxpicture.domain.picture.service.PictureDomainService;
import com.zzx.zzxpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zzx.zzxpicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.zzx.zzxpicture.interfaces.dto.picture.*;
import com.zzx.zzxpicture.shared.auth.SpaceUserAuthManager;
import com.zzx.zzxpicture.shared.auth.StpKit;
import com.zzx.zzxpicture.infrastructure.constant.RedisConstant;
import com.zzx.zzxpicture.domain.space.constant.SpaceUserPermissionConstant;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.interfaces.dto.analyze.SpaceSizeAnalyzeRequest;
import com.zzx.zzxpicture.domain.picture.valueobject.PictureReviewEnum;
import com.zzx.zzxpicture.domain.space.entity.Space;
import com.zzx.zzxpicture.domain.picture.entity.Picture;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.interfaces.vo.picture.PictureVO;
import com.zzx.zzxpicture.application.service.PictureApplicationService;
import com.zzx.zzxpicture.infrastructure.mapper.PictureMapper;
import com.zzx.zzxpicture.application.service.SpaceApplicationService;
import com.zzx.zzxpicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 28299
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-09-13 17:08:54
 */
@Service
@Slf4j
public class PictureApplicationServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureApplicationService {


    @Resource
    private PictureDomainService pictureDomainService;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    // 引入 redis
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 本地 caffeine 缓存
    @Resource
    private Cache<String, String> localCache;

    // 引入权限管理
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        return pictureDomainService.uploadPicture(inputSource, pictureUploadRequest, loginUser);
    }

    /**
     * 删除图片
     *
     * @param pictureId 图片id
     * @param request
     * @return 删除成功与否
     */
    @Override
    public Boolean deletePicture(Long pictureId, HttpServletRequest request) {
        return pictureDomainService.deletePicture(pictureId, request);
    }


    /**
     * 更新图片信息 （管理员）
     *
     * @param pictureUpdateRequest
     * @return
     */
    @Override
    public Boolean updatePicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // 判断照片是否存在
        Picture picture = this.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 更新图片信息
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        // 补充审核参数
        this.fileReviewParams(picture, userApplicationService.getLoginUser(request));
        // 设置tags
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // boolean result = this.updateById(picture);
        // 补充条件修改 spaceId = 0
        UpdateWrapper<Picture> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", pictureUpdateRequest.getId())
                .eq("spaceId", picture.getSpaceId());
        boolean result = this.update(picture, updateWrapper);
        return result;
    }

    /**
     * 修改照片（用户）
     *
     * @param pictureUpdateRequest
     * @return
     */
    @Override
    public Boolean editPicture(PictureEditRequest pictureUpdateRequest, HttpServletRequest request) {
        User user = userApplicationService.getLoginUser(request);
        return pictureDomainService.editPicture(pictureUpdateRequest, user);
    }

    /**
     * 获取图片VO信息
     *
     * @param id
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVOById(Long id, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        // 获取图片信息
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        Space space = null;
        // 补充查询权限 spaceId != 0
        if (spaceId != null && spaceId != 0L) {
            boolean b = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!b, ErrorCode.NO_AUTH_ERROR);
            // 已经改为 Sa-Token 校验
            User loginUser = userApplicationService.getLoginUser(request);
            // checkPictureAuth(loginUser, picture);
            // 获取用户权限
            space = spaceApplicationService.getById(spaceId);
        }
        // 获取到权限列表
        User loginUser = userApplicationService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 封装返回
        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        pictureVO.setUserVO(userApplicationService.getUserVO(userApplicationService.getUserById(picture.getUserId())));
        pictureVO.setPermissionList(permissionList);
        return pictureVO;
    }

    /**
     * 获取查询条件
     *
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.getQueryWrapper(pictureQueryRequest);
    }

    /**
     * 分页查询VO （用户查询）
     *
     * @param current             当前页
     * @param size                每页大小
     * @param pictureQueryRequest 查询条件
     * @return
     */
    public Page<PictureVO> getPictureVOPage(Long current, Long size, PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 首先要空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库需要进行缓存，并且只能查看已经过审的公开数据
        // 构造缓存key
        String pictureQueryJsonStr = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(pictureQueryJsonStr.getBytes());
        String key = RedisConstant.PICTURE_SELECT_KEY + "getPicturePage:" + hashKey;
        if (spaceId == null || spaceId == 0L) {
            // 普通用户只能看到审核通过的照片
            pictureQueryRequest.setReviewStatus(PictureReviewEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
            // 1. 先从本地 Caffeine 缓存查询
            // 查询缓存
            String cachedValue = localCache.getIfPresent(key);
            if (StrUtil.isNotBlank(cachedValue)) {
                // 缓存命中，直接返回
                Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
                return cachePage;
            }
            // 2. 本地 Caffeine 缓存未命中，查询 redis 缓存
            String pictureVOPageStr = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(pictureVOPageStr)) {
                // 缓存命中，直接返回
                Page<PictureVO> cachePage = JSONUtil.toBean(pictureVOPageStr, Page.class);
                return cachePage;
            }
        } else {
            boolean b = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!b, ErrorCode.NO_AUTH_ERROR);
            // 私有空间不需要进行缓存
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 已改为统一校验权限
            /* if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            } */
        }

        // 3. 两级缓存都没有命中，查询数据库，获取图片列表和创建VO分页对象
        Page<Picture> picturePage = this.page(new Page<>(current, size), this.getQueryWrapper(pictureQueryRequest));
        List<Picture> pictureList = picturePage.getRecords(); // 图片列表
        // ThrowUtils.throwIf(CollUtil.isEmpty(pictureList), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 创建VO分页对象
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 3.1 如果是访问公共空间，则需要缓存图片列表，解决缓存穿透
        if (spaceId == null || spaceId == 0L) {
            if (CollUtil.isEmpty(pictureList)) {
                // 设置随机过期时间 3 - 5 分钟，避免缓存雪崩
                int randomTime = 180 + RandomUtil.randomInt(120);
                stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(pictureVOPage), randomTime, TimeUnit.SECONDS);
                // 返回空的VO分页对象
                return pictureVOPage;
            }
        }

        // 4. 封装VO列表：将Picture对象转换为PictureVO对象
        List<PictureVO> pictureVOList = pictureList.stream().map(picture -> {
            PictureVO pictureVO = new PictureVO();
            BeanUtil.copyProperties(picture, pictureVO);
            // 3.1 将JSON格式的标签字符串转换为List<String>
            pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
            return pictureVO;
        }).collect(Collectors.toList());

        // 5. 查询关联用户信息
        // 5.1 提取所有图片的用户ID集合
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 5.2 根据用户ID批量查询用户信息，并按ID分组
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 6. 为每个图片VO设置用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            // 6.1 根据用户ID获取对应的用户信息
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            // 6.2 设置用户VO信息
            pictureVO.setUserVO(userApplicationService.getUserVO(user));
        });

        // 7. 将转换后的VO列表设置到VO分页对象中
        pictureVOPage.setRecords(pictureVOList);

        // 如果是访问公共空间，则需要缓存图片列表
        if (spaceId == null || spaceId == 0L) {
            // 存入本地 Caffeine 缓存
            localCache.put(key, JSONUtil.toJsonStr(pictureVOPage));

            // 存入 redis 缓存 5 - 10 分钟
            int randomTime = 300 + RandomUtil.randomInt(300);
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(pictureVOPage), randomTime, TimeUnit.SECONDS);
        }

        // 8. 返回封装好的图片VO分页数据
        return pictureVOPage;
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @Override
    public Boolean doPictureReview(PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        // 1. 获得当前登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        return pictureDomainService.doPictureReview(pictureReviewRequest, loginUser);
    }

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fileReviewParams(Picture picture, User loginUser) {
        pictureDomainService.fileReviewParams(picture, loginUser);
    }

    /**
     * 批量抓取图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功的图片数量
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        return pictureDomainService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
    }


    /**
     * 清理图片文件
     *
     * @param oldPicture
     */
    @Override
    public void clearPictureFile(Picture oldPicture) {
        pictureDomainService.clearPictureFile(oldPicture);
    }

    /**
     * 搜索图片
     *
     * @param searchPictureByColorRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(SearchPictureByColorRequest searchPictureByColorRequest, User loginUser) {
        return pictureDomainService.searchPictureByColor(searchPictureByColorRequest, loginUser);
    }

    /**
     * 批量修改图片信息
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        pictureDomainService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
    }

    /**
     * 创建 AI 扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        return pictureDomainService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
    }

    /**
     * 获取 AI 扩图任务结果
     *
     * @param taskId
     * @return
     */
    @Override
    public GetOutPaintingTaskResponse getPictureOutPaintingTask(String taskId) {
        return pictureDomainService.getPictureOutPaintingTask(taskId);
    }


    /**
     * 获取空间图片大小
     *
     * @param spaceSizeAnalyzeRequest 空间图片大小分析请求参数
     * @return
     */
    @Override
    public List<Long> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest) {
        return this.getBaseMapper().selectSpaceSizeAnalyze(spaceSizeAnalyzeRequest);
    }

}
