package com.zzx.zzxpicture.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.zzxpicture.infrastructure.annotation.AuthCheck;
import com.zzx.zzxpicture.infrastructure.common.BaseResponse;
import com.zzx.zzxpicture.infrastructure.common.DeleteRequest;
import com.zzx.zzxpicture.infrastructure.common.ResultUtils;
import com.zzx.zzxpicture.infrastructure.exception.BusinessException;
import com.zzx.zzxpicture.infrastructure.exception.ErrorCode;
import com.zzx.zzxpicture.infrastructure.exception.ThrowUtils;
import com.zzx.zzxpicture.interfaces.dto.user.*;
import com.zzx.zzxpicture.domain.user.valueobject.UserRoleEnum;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.interfaces.vo.user.LoginUserVO;
import com.zzx.zzxpicture.interfaces.vo.user.UserVO;
import com.zzx.zzxpicture.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册参数
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long userId = userApplicationService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录参数
     * @param request          请求
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userApplicationService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获得当前登录的用户
     */
    @GetMapping("/get/user")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userApplicationService.getLoginUser(request);
        return ResultUtils.success(userApplicationService.getLoginUserVO(user));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        Boolean result = userApplicationService.userLogout(request);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        } else {
            return ResultUtils.success(result);
        }
    }

    /**
     * 添加用户（管理员）
     */
    @PostMapping("/add")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        Long userId = userApplicationService.addUser(userAddRequest);
        return ResultUtils.success(userId);
    }

    /**
     * 删除用户（管理员）
     *
     * @param deleteRequest 删除参数
     * @return 删除结果
     */
    @PostMapping("/delete")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userApplicationService.deleteUser(deleteRequest);
        return ResultUtils.success(result);
    }

    /**
     * 更新用户信息（管理员）
     *
     * @param userUpdateRequest 更新参数
     * @return 更新结果
     */
    @PostMapping("/update")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userApplicationService.updateUser(userUpdateRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(result);
    }


    /**
     * 根据 id 获取用户（管理员）
     *
     * @param id 查询参数
     * @return 用户列表
     */
    @GetMapping("/get")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<User> getUserById(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.getUserById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户脱敏信息
     *
     * @param id 查询参数
     * @return 用户列表
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(@RequestParam Long id) {
        User user = this.getUserById(id).getData();
        UserVO userVO = UserVO.objToVo(user);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取用户列表（管理员）
     *
     * @param userQueryRequest 查询参数
     * @return 用户列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(value = UserRoleEnum.ADMIN)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        Page<UserVO> userVOPage = userApplicationService.listUserVOByPage(userQueryRequest);
        return ResultUtils.success(userVOPage);
    }

}
