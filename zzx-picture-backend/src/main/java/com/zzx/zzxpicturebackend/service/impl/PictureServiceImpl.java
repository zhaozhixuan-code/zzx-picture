package com.zzx.zzxpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.zzx.zzxpicturebackend.api.aliyunai.AliyunAiApi;
import com.zzx.zzxpicturebackend.api.aliyunai.model.*;
import com.zzx.zzxpicturebackend.api.aliyunai.model.TextToImageRequest;
import com.zzx.zzxpicturebackend.manager.auth.SpaceUserAuthManager;
import com.zzx.zzxpicturebackend.manager.auth.StpKit;
import com.zzx.zzxpicturebackend.constant.RedisConstant;
import com.zzx.zzxpicturebackend.constant.SpaceUserPermissionConstant;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import com.zzx.zzxpicturebackend.model.dto.analyze.SpaceSizeAnalyzeRequest;
import com.zzx.zzxpicturebackend.model.dto.picture.*;
import com.zzx.zzxpicturebackend.model.enums.PictureReviewEnum;
import com.zzx.zzxpicturebackend.model.enums.UserRoleEnum;
import com.zzx.zzxpicturebackend.model.po.Space;
import com.zzx.zzxpicturebackend.model.vo.UploadPictureResult;
import com.zzx.zzxpicturebackend.model.po.Picture;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.PictureVO;
import com.zzx.zzxpicturebackend.model.vo.analyze.SpaceCategoryAnalyzeResponse;
import com.zzx.zzxpicturebackend.service.PictureService;
import com.zzx.zzxpicturebackend.mapper.PictureMapper;
import com.zzx.zzxpicturebackend.service.SpaceService;
import com.zzx.zzxpicturebackend.service.UserService;
import com.zzx.zzxpicturebackend.utils.ColorSimilarUtils;
import com.zzx.zzxpicturebackend.utils.upload.FilePictureUpload;
import com.zzx.zzxpicturebackend.utils.upload.PictureUploadTemplate;
import com.zzx.zzxpicturebackend.utils.upload.UrlPictureUpload;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.awt.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 28299
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-09-13 17:08:54
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {


    // @Resource
    // private CosUtil cosUtil;
    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    // 引入 redis
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 本地 caffeine 缓存
    @Resource
    private Cache<String, String> localCache;

    // 引入线程池
    @Resource
    private ThreadPoolExecutor customExecutor;

    // 引入阿里云
    @Resource
    private AliyunAiApi aliyunAiApi;

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
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null && spaceId != 0L) {
            // 表示要上传到私人空间
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 只有空间的创建者可上传
            // 改为统一的校验权限
/*             if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            } */
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间已满");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间已满");
            }
        }
        // 用于判断是新增还是更新照片
        Long pictureId;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        } else {
            pictureId = null;
        }
        // 如果是更新照片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或者管理员可编辑
            // 改为统一的权限校验
            /* UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            } */
            // boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId).exists();
            // ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 校验空间是否一致
            // 没有上传spaceId, 测试用原图片的spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 上传了spaceId,必须和原图片一致
                if (!oldPicture.getSpaceId().equals(spaceId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间不一致");
                }
            }
        }
        // 上传图片，得到信息
        // 需要按照用户的id 划分目录
        String uploadPathPrefix = null;
        if (spaceId == null || spaceId == 0L) {
            // 上传到公共空间
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 类别区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        // 上传图片
        // UploadPictureResult uploadPictureResult = cosUtil.uploadPicture(file, uploadPathPrefix);
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 存入数据库
        Picture picture = new Picture();
        // 补充设置 spaceId ，为了兼容分表逻辑，默认为0
        picture.setSpaceId(spaceId == null ? 0L : spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 补充设置 originalUrl
        picture.setOriginalUrl(uploadPictureResult.getOriginalUrl());
        picture.setPicColor(uploadPictureResult.getPicColor());
        String picName = uploadPictureResult.getPicName();
        // 存入照片名称
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        // 存入照片分类
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getCategory())) {
            picture.setCategory(pictureUploadRequest.getCategory());
        }
        // 存入照片分类
        if (pictureUploadRequest != null && CollUtil.isNotEmpty(pictureUploadRequest.getTags())) {
            picture.setTags(JSONUtil.toJsonStr(pictureUploadRequest.getTags()));
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 补充审核参数
        this.fileReviewParams(picture, loginUser);
        // 开启事务，保存图片和空间额度
        Long finalSpaceId = picture.getSpaceId();
        transactionTemplate.execute(status -> {
            // 保存图片
            // boolean result = this.saveOrUpdate(picture);
            boolean result;
            // 替换原来的 saveOrUpdate 调用 ,添加spaceId为更改条件
            if (pictureId != null) {
                // 更新操作
                QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("id", pictureId)
                        .eq("spaceId", finalSpaceId);
                result = this.update(picture, queryWrapper);
            } else {
                // 新增操作
                result = this.save(picture);
            }

            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存图片失败");
            // 更新额度
            if (finalSpaceId != null && finalSpaceId != 0L) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + uploadPictureResult.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新空间额度失败");
            }
            return true;
        });

        // 转换成vo对象返回
        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);
        // 获取标签，并且转换成List<String>对象返回
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
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
        // 校验参数
        ThrowUtils.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR);
        // 获取照片，判断数据库中是否存在该图片
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取用户id，仅本人或者管理员才能删除
        User loginUser = userService.getLoginUser(request);
        // ThrowUtils.throwIf(!(user.getId().equals(oldPicture.getUserId()) || user.getUserRole().equals("admin")), ErrorCode.NO_AUTH_ERROR);
        // 校验权限 已经改为 Sa-Token 校验
        // checkPictureAuth(loginUser, oldPicture);
        // 开启事务，需要删除图片和修改空间额度
        transactionTemplate.execute(status -> {
            // 删除图片
            // 删除图片时，补充设置spaceId作为查询条件
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", pictureId)
                    .eq("spaceId", oldPicture.getSpaceId());
            boolean result = this.remove(queryWrapper);
            // boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除图片失败");
            // 修改空间额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null && spaceId != 0L) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新空间额度失败");
            }
            return true;
        });

        // 移除图片存储
        this.clearPictureFile(oldPicture);
        return true;
    }

    /**
     * 编辑图片和删除图片的校验
     * 只有用户自己可以编辑图片和删除图片
     *
     * @param loginUser
     * @param picture
     */
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库, 只有创建人和管留言可以操作
            if (!picture.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私人空间, 只有空间的管理人可以操作
            Long userId = picture.getUserId();
            if (!userId.equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
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
        this.fileReviewParams(picture, userService.getLoginUser(request));
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
        // 校验参数
        ThrowUtils.throwIf(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // 判断照片是否存在
        Picture picture = this.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验身份，只有用户本人或者管理员才能编辑
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(!(user.getId().equals(picture.getUserId()) || "admin".equals(user.getUserRole())), ErrorCode.NO_AUTH_ERROR);
        // 更改照片信息（基于原图片对象进行更新）
        // 补充审核参数
        this.fileReviewParams(picture, user);
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        picture.setEditTime(new Date());
        // 补充审核参数
        this.fileReviewParams(picture, user);
        // boolean result = this.updateById(picture);
        // 补充条件修改 spaceId = 0
        UpdateWrapper<Picture> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", pictureUpdateRequest.getId())
                .eq("spaceId", picture.getSpaceId());
        boolean result = this.update(picture, updateWrapper);
        return result;
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
            User loginUser = userService.getLoginUser(request);
            // checkPictureAuth(loginUser, picture);
            // 获取用户权限
            space = spaceService.getById(spaceId);
        }
        // 获取到权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 封装返回
        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        pictureVO.setUserVO(userService.getUserVO(userService.getById(picture.getUserId())));
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
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date reviewTime = pictureQueryRequest.getReviewTime();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        // 修改空间查询条件，如果查询公共图库应该为 0
        queryWrapper.eq(nullSpaceId, "spaceId", 0L);
        // queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.le(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
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
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
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
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
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
            pictureVO.setUserVO(userService.getUserVO(user));
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
        User loginUser = userService.getLoginUser(request);
        // 2. 参数校验
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewEnum reviewStatusEnum = PictureReviewEnum.getEnumByValue(reviewStatus);
        Long pictureId = pictureReviewRequest.getId();
        // 前端传过来的状态只能是通过或者拒绝，所以需要判断状态
        if (reviewStatusEnum == null || pictureId == null || PictureReviewEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 3. 判断照片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 4. 判断照片已经是要修改的状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 5. 更新审核状态
        // new一个新对象，减少更新的字段
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        // 6. 返回
        return result;
    }

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fileReviewParams(Picture picture, User loginUser) {
        // 管理员自动审核通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            picture.setReviewStatus(PictureReviewEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员审核通过");
        } else {
            picture.setReviewStatus(PictureReviewEnum.REVIEWING.getValue());
        }

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
        // 1、获取参数
        String searchText = pictureUploadByBatchRequest.getSearchText(); // 搜索关键词
        Integer maxNum = pictureUploadByBatchRequest.getCount(); // 要抓取的图片数量
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        searchText += "4K";
        /* ---------- 2. 抓取列表 ---------- */
        String listUrl = StrUtil.format("https://cn.bing.com/images/async?q={}&mmasync=1",
                URLEncoder.encode(searchText, StandardCharsets.UTF_8));
        Document doc;
        try {
            doc = Jsoup.connect(listUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片列表抓取失败");
        }

        /* ---------- 3. 提取原图 ---------- */
        Elements links = doc.select("a.iusc");   // 每一张图的外层链接
        int success = 0;
        for (Element a : links) {
            if (success >= maxNum) break;

            String originalUrl = parseOriginalUrl(a.attr("m")); // 核心：拿原图
            if (StrUtil.isBlank(originalUrl)) continue;

            /* ---------- 4. 上传 ---------- */
            PictureUploadRequest req = new PictureUploadRequest();
            req.setFileUrl(originalUrl);
            req.setPicName(StrUtil.isNotBlank(namePrefix)
                    ? namePrefix + (success + 1)
                    : "pic" + (success + 1));
            req.setCategory(pictureUploadByBatchRequest.getCategory());
            req.setTags(pictureUploadByBatchRequest.getTags());
            try {
                this.uploadPicture(originalUrl, req, loginUser);
                success++;
            } catch (Exception e) {
                log.warn("单张上传失败，已跳过：{}", originalUrl);
            }
        }
        return success;
    }


    /**
     * 清理图片文件
     *
     * @param oldPicture
     */
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该照片是否被多条记录使用
        String url = oldPicture.getUrl();
        Long count = this.lambdaQuery().eq(Picture::getUrl, url).count();
        // 有多个文件使用了该照片的url，则不删除
        if (count > 1) {
            return;
        }
        // 删除压缩后的文件（webp）
        // 我们的数据库存储的只有压缩后的文件，所以只能删除压缩后的图片
        filePictureUpload.deleteObject(oldPicture.getUrl());
        // 删除缩略图
        if (StrUtil.isNotBlank(oldPicture.getThumbnailUrl())) {
            filePictureUpload.deleteObject(oldPicture.getThumbnailUrl());
        }
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
        // 1. 获取参数
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(picColor) || spaceId == null || spaceId == 0L, ErrorCode.PARAMS_ERROR);
        // 校验空间权限
        Space space = spaceService.getById(spaceId);
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 查询该空间下的所有照片（必须包含颜色字段的）
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 如果没有图片，直接返回空列表
        if (pictureList.isEmpty()) {
            return Collections.emptyList();
        }
        // 以目标颜色为基准
        Color targetColor = Color.decode(picColor);
        // 计算颜色的相似度并且排序
        List<Picture> sortedPictures = pictureList.stream()
                .filter(picture -> {
                    String hexColor = picture.getPicColor();
                    if (StrUtil.isBlank(hexColor)) {
                        return false;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    double similarity = ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                    return similarity > 0.6; // 只保留相似度高于60%的图片
                })
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    Color pictureColor = Color.decode(hexColor);
                    // 越大越相似，使用负值实现降序排序
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .collect(Collectors.toList());
        List<PictureVO> pictureVOList = sortedPictures.stream()
                .map(PictureVO::poToVo)
                .collect(Collectors.toList());
        return pictureVOList;
    }

    /**
     * 批量修改图片信息
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList) || spaceId == null || spaceId == 0L, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        // 查询图片是否存在
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList.size() != pictureIdList.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "部分图片不存在");
        }
        if (pictureList.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 分批处理避免长事务
        int batchSize = 100;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < pictureList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, pictureList.size());
            List<Picture> batchPictures = pictureList.subList(i, end);

            int startIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                batchPictures.forEach(picture -> {
                    if (StrUtil.isNotEmpty(category)) {
                        picture.setCategory(category);
                    }
                    if (CollUtil.isNotEmpty(tags)) {
                        picture.setTags(JSONUtil.toJsonStr(tags));
                    }
                });
                fillPictureWithNameRule(batchPictures, nameRule, startIndex);
                // 批量更新
                boolean update = this.updateBatchById(batchPictures);
                ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "批量修改图片信息失败");
            }, customExecutor);
            futures.add(future);
        }
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
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
        // 获取图片信息
        Picture picture = this.getById(createPictureOutPaintingTaskRequest.getPictureId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 校验权限（公共图库：只有创建人和管留言可以操作，私有图库：只有创建人可以操作）
        // 已经改为 Sa-Token 校验
        // this.checkPictureAuth(loginUser, picture);
        // 构造请求参数
        CreateOutPaintingTaskRequest request = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getOriginalUrl());
        request.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, request);
        // 发送请求
        CreateOutPaintingTaskResponse response = aliyunAiApi.createOutPaintingTask(request);
        // 返回
        return response;
    }

    /**
     * 获取 AI 扩图任务结果
     *
     * @param taskId
     * @return
     */
    @Override
    public GetOutPaintingTaskResponse getPictureOutPaintingTask(String taskId) {
        GetOutPaintingTaskResponse outPaintingTask = aliyunAiApi.getOutPaintingTask(taskId);
        return outPaintingTask;
    }


    /**
     * 获取文生图
     *
     * @param request
     * @return
     */
    @Override
    public TextToImageResponse getTextToImage(TextToImageRequest request) {
        TextToImageResponse textToImage = aliyunAiApi.getTextToImage(request);
        return textToImage;
    }


    /**
     * 分类统计
     *
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getCategoryStats() {
        return this.getBaseMapper().selectCategoryStats();
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


    /**
     * 批量填充图片名称
     *
     * @param batchPictures
     * @param nameRule
     * @param startIndex
     */
    private void fillPictureWithNameRule(List<Picture> batchPictures, String nameRule, int startIndex) {
        // 校验参数
        if (CollUtil.isEmpty(batchPictures)) {
            return;
        }
        if (StrUtil.isBlank(nameRule)) {
            return;
        }
        int index = startIndex + 1;
        try {
            for (Picture picture : batchPictures) {
                String pictureName = nameRule.replaceAll("\\{index}", String.valueOf(index++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量填充图片名称失败");
        }
    }

    /**
     * 解析爬虫地址，从 m 属性里抽 murl
     *
     * @param mJson
     * @return
     */
    private String parseOriginalUrl(String mJson) {
        if (StrUtil.isBlank(mJson)) return null;
        // 最简单可靠：字符串截取，避免引入 JSON 库
        String key = "\"murl\":\"";
        int start = mJson.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = mJson.indexOf('"', start);
        if (end == -1) return null;
        return mJson.substring(start, end)
                .replace("\\/", "/");
    }
}




