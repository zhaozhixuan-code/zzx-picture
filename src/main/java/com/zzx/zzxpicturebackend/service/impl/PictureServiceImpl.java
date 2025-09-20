package com.zzx.zzxpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.exception.ThrowUtils;
import com.zzx.zzxpicturebackend.model.dto.picture.*;
import com.zzx.zzxpicturebackend.model.enums.PictureReviewEnum;
import com.zzx.zzxpicturebackend.model.enums.UserRoleEnum;
import com.zzx.zzxpicturebackend.model.vo.UploadPictureResult;
import com.zzx.zzxpicturebackend.model.po.Picture;
import com.zzx.zzxpicturebackend.model.po.User;
import com.zzx.zzxpicturebackend.model.vo.PictureVO;
import com.zzx.zzxpicturebackend.service.PictureService;
import com.zzx.zzxpicturebackend.mapper.PictureMapper;
import com.zzx.zzxpicturebackend.service.UserService;
import com.zzx.zzxpicturebackend.utils.upload.FilePictureUpload;
import com.zzx.zzxpicturebackend.utils.upload.PictureUploadTemplate;
import com.zzx.zzxpicturebackend.utils.upload.UrlPictureUpload;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        // 用于判断是新增还是更新照片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是新增照片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或者管理员可编辑
            UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
            if (oldPicture.getUserId().equals(loginUser.getId()) || !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId).exists();
            // ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 上传图片，得到信息
        // 需要按照用户的id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
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
        picture.setUrl(uploadPictureResult.getUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
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
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存图片失败");
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
     * @param id      图片id
     * @param request
     * @return 删除成功与否
     */
    @Override
    public Boolean deletePicture(Long id, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        // 获取照片，判断数据库中是否存在该图片
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取用户id，仅本人或者管理员才能删除
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(!(user.getId().equals(picture.getUserId()) || user.getUserRole().equals("admin")), ErrorCode.NO_AUTH_ERROR);
        // 删除图片
        boolean result = this.removeById(id);
        return result;
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
        boolean result = this.updateById(picture);
        return result;
    }

    /**
     * 修改照片（用户）
     *
     * @param pictureUpdateRequest
     * @return
     */
    @Override
    public Boolean editPicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
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
        boolean result = this.updateById(picture);
        return result;
    }

    /**
     * 获取图片VO信息
     *
     * @param id
     * @return
     */
    @Override
    public PictureVO getPictureVOById(Long id) {
        // 参数校验
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        // 获取图片信息
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 封装返回
        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        pictureVO.setUserVO(userService.getUserVO(userService.getById(picture.getUserId())));
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
        Date reviewTime = pictureQueryRequest.getReviewTime();
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
     * 获取图片VO列表
     *
     * @param picturePage
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 封装VO列表
        List<PictureVO> pictureVOList = pictureList.stream().map(picture -> {
            PictureVO pictureVO = new PictureVO();
            BeanUtil.copyProperties(picture, pictureVO);
            pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
            // pictureVO.setUserVO(userService.getUserVO(userService.getById(picture.getUserId())));
            return pictureVO;
        }).collect(Collectors.toList());

        // 查询关联用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 用户id对应一个用户
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUserVO(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);


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
     * 从 m 属性里抽 murl
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




