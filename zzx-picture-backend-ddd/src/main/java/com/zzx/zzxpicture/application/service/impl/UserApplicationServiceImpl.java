package com.zzx.zzxpicture.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.zzxpicture.domain.user.service.UserDomainService;
import com.zzx.zzxpicture.infrastructure.common.DeleteRequest;
import com.zzx.zzxpicture.infrastructure.exception.BusinessException;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.interfaces.dto.user.UserAddRequest;
import com.zzx.zzxpicture.interfaces.dto.user.UserQueryRequest;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.interfaces.dto.user.UserUpdateRequest;
import com.zzx.zzxpicture.interfaces.vo.user.LoginUserVO;
import com.zzx.zzxpicture.interfaces.vo.user.UserVO;
import com.zzx.zzxpicture.application.service.UserApplicationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


/**
 * @author 28299
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-09-09 21:11:30
 */
@Service
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

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
        // 校验参数
        User.validUserRegister(userAccount, userPassword, checkPassword);
        // 执行
        return userDomainService.userRegister(userAccount, userPassword, checkPassword);
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
        // 1. 校验
        User.validUserLogin(userAccount, userPassword);
        // 执行
        return userDomainService.userLogin(userAccount, userPassword, request);
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     */
    @Override
    public String getEncryptionPassword(String userPassword) {
        return userDomainService.getEncryptionPassword(userPassword);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 登录请求
     * @return 当前登录用户脱敏后的信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }


    /**
     * 用户脱敏
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return userDomainService.getLoginUserVO(user);
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return userDomainService.getUserVOList(userList);
    }


    /**
     * 用户退出登录
     *
     * @param request HTTP请求对象，用于存储登录状态
     * @return 退出登录是否成功
     */
    @Override
    public Boolean userLogout(HttpServletRequest request) {
        return userDomainService.userLogout(request);
    }


    /**
     * 添加用户
     *
     * @param userAddRequest 添加用户请求参数
     * @return 新用户ID
     */
    @Override
    public Long addUser(UserAddRequest userAddRequest) {
        return userDomainService.addUser(userAddRequest);
    }

    /**
     * 获取查询条件
     *
     * @param userQueryRequest 查询条件请求参数
     * @return 查询条件
     */
    @Override
    public Wrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }

    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public User getUserById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids){
        List<User> users = userDomainService.listByIds(ids);
        return users;
    }

    /**
     * 删除用户
     *
     * @param deleteRequest 删除请求参数
     * @return 删除结果
     */
    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userDomainService.removeById(deleteRequest.getId());
        return result;
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest 更新请求参数
     * @return 更新结果
     */
    @Override
    public boolean updateUser(UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userDomainService.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return result;
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userDomainService.page(new Page<>(current, size),
                userDomainService.getQueryWrapper(userQueryRequest));
        List<UserVO> userVOList = userDomainService.getUserVOList(userPage.getRecords());
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }

}




