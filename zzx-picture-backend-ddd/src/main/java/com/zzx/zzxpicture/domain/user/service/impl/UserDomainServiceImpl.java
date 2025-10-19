package com.zzx.zzxpicture.domain.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.zzxpicture.domain.user.service.UserDomainService;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.domain.user.repository.UserRepository;
import com.zzx.zzxpicture.infrastructure.exception.BusinessException;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.infrastructure.utils.BaseContext;
import com.zzx.zzxpicture.interfaces.dto.user.UserAddRequest;
import com.zzx.zzxpicture.interfaces.dto.user.UserQueryRequest;
import com.zzx.zzxpicture.interfaces.vo.user.LoginUserVO;
import com.zzx.zzxpicture.interfaces.vo.user.UserVO;
import com.zzx.zzxpicture.shared.auth.StpKit;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.zzx.zzxpicture.domain.user.constant.UserConstant.DEFAULT_PASSWORD;
import static com.zzx.zzxpicture.domain.user.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author 28299
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-09-09 21:11:30
 */
@Service
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    private UserRepository userRepository;

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 2. 检查是否存在该账户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userRepository.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户已存在");
        }
        getEncryptionPassword(userPassword);
        // 3. 加密密码
        userPassword = getEncryptionPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(userPassword);
        boolean isSave = userRepository.save(user);
        if (!isSave) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return 0;
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     */
    @Override
    public String getEncryptionPassword(String userPassword) {
        String salt = "zzxpicturebackend";
        return DigestUtils.md5DigestAsHex((salt + userPassword).getBytes());
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      请求
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 2. 检查账户是否存在
        // 获取加密密码
        String encryptionPassword = getEncryptionPassword(userPassword);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptionPassword);
        User user = userRepository.getOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 存入ThreadLocal
        BaseContext.setCurrentId(user.getId());
        // 4. 记录用户登录态到 Sa-Token
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        // 5. 返回脱敏后的用户信息
        LoginUserVO loginUserVo = this.getLoginUserVO(user);
        return loginUserVo;
    }

    /**
     * 用户脱敏
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        LoginUserVO loginUserVo = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVo);
        return loginUserVo;
    }


    /**
     * 获取当前登录用户
     *
     * @param request 登录请求
     * @return 当前登录用户脱敏后的信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        // 未登录
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    /**
     * 用户退出登录
     *
     * @param request HTTP请求对象，用于存储登录状态
     * @return 退出登录是否成功
     */
    @Override
    public Boolean userLogout(HttpServletRequest request) {
        // 先判断是否已经登录了
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 退出登录
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        // 从ThreadLocal移除当前用户ID
        BaseContext.removeCurrentId();
        return true;
    }


    /**
     * 添加用户
     *
     * @param userAddRequest 添加用户请求参数
     * @return 新用户ID
     */
    @Override
    public Long addUser(UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        user.setUserPassword(this.getEncryptionPassword(DEFAULT_PASSWORD));
        boolean result = userRepository.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return user.getId();
    }

    /**
     * 获取查询条件
     *
     * @param userQueryRequest 查询条件请求参数
     * @return 查询条件
     */
    @Override
    public Wrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public boolean removeById(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public User getById(Long id){
        return userRepository.getById(id);
    }

    @Override
    public List<User> listByIds(Collection<? extends Serializable> idList){
        return userRepository.listByIds(idList);
    }

    @Override
    public boolean updateById(User user){
        return userRepository.updateById(user);
    }

    @Override
    public Page<User> page(Page<User> page, Wrapper<User> queryWrapper){
        return userRepository.page(page, queryWrapper);
    }
}




