package com.zzx.zzxpicture.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.interfaces.dto.user.UserAddRequest;
import com.zzx.zzxpicture.interfaces.dto.user.UserQueryRequest;
import com.zzx.zzxpicture.interfaces.vo.user.LoginUserVO;
import com.zzx.zzxpicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author 28299
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-09-09 21:11:30
 */
public interface UserDomainService {


    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 新注册用户的ID
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密密码
     *
     * @param userPassword 明文密码
     * @return 加密后的密码
     */
    String getEncryptionPassword(String userPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      HTTP请求对象，用于存储登录状态
     * @return 登录用户信息的UserVo对象
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 将User实体转换为UserVo对象
     *
     * @param user User实体对象
     * @return UserVo视图对象
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     * 获取当前登录用户信息
     *
     * @param request HTTP请求对象，用于存储登录状态
     * @return 当前登录用户信息的UserVo对象
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 用户退出登录
     *
     * @param request HTTP请求对象，用于存储登录状态
     * @return 是否注销成功
     */
    Boolean userLogout(HttpServletRequest request);


    /**
     * 添加用户
     *
     * @param userAddRequest
     */
    Long addUser(UserAddRequest userAddRequest);

    /**
     * 获取查询条件包装器
     *
     * @param userQueryRequest
     * @return
     */
    Wrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);


    boolean removeById(Long id);

    User getById(Long id);

    List<User> listByIds(Collection<? extends Serializable> idList);

    boolean updateById(User user);

    Page<User> page(Page<User> page, Wrapper<User> queryWrapper);
}
