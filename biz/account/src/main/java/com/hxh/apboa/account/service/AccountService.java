package com.hxh.apboa.account.service;

import com.hxh.apboa.common.dto.*;
import com.hxh.apboa.common.entity.Account;
import com.hxh.apboa.common.enums.Role;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 账号Service
 *
 * @author huxuehao
 */
public interface AccountService extends IService<Account> {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 是否成功
     */
    boolean register(RegisterRequest request);

    /**
     * 刷新Token
     *
     * @param request 刷新请求
     * @return 登录响应
     */
    LoginResponse refreshToken(RefreshTokenRequest request);

    /**
     * 用户退出
     */
    void logout();

    /**
     * 修改密码
     *
     * @param request 修改密码请求
     * @return 是否成功
     */
    boolean changePassword(ChangePasswordRequest request);

    /**
     * 修改个人信息
     *
     * @param request 修改个人信息请求
     * @return 是否成功
     */
    boolean updateProfile(UpdateProfileRequest request);

    /**
     * 禁用/激活用户
     *
     * @param id 用户ID
     * @param enabled 是否启用
     * @return 是否成功
     */
    boolean toggleEnabled(Long id, Boolean enabled);

    /**
     * 管理员修改用户密码
     *
     * @param id 用户ID
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean adminChangePassword(Long id, String newPassword);

    /**
     * 修改角色
     * @param id    用户ID
     * @param roles 角色
     */
    boolean changeRole(Long id, List<Role> roles);

    /**
     * 通过ChatKey换取Token
     *
     * @param chatKey 对话Key
     * @return token，如果验证失败返回null
     */
    LoginResponse chatKeyToken(String chatKey);
}
