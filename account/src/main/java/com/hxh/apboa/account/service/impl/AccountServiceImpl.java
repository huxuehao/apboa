package com.hxh.apboa.account.service.impl;

import com.hxh.apboa.account.mapper.AccountMapper;
import com.hxh.apboa.account.service.AccountRoleService;
import com.hxh.apboa.account.service.AccountService;
import com.hxh.apboa.common.UserDetail;
import com.hxh.apboa.common.config.auth.AuthInterceptor;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.dto.*;
import com.hxh.apboa.common.entity.Account;
import com.hxh.apboa.common.entity.AccountRole;
import com.hxh.apboa.common.enums.Role;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.common.exception.NotAuthException;
import com.hxh.apboa.common.message.AccountRoleChangeMessage;
import com.hxh.apboa.common.util.*;
import com.hxh.apboa.ws.config.ApboaWebSocketSessionManager;
import com.hxh.apboa.ws.context.ApboaWebSocketSession;
import com.hxh.apboa.ws.handler.ServiceMessageHandlerAdapter;
import com.hxh.apboa.ws.handler.server.ServerMessageHandler;
import com.hxh.apboa.ws.model.WsServerMessage;
import com.hxh.apboa.params.core.ParamsAdapter;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 账号Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    private final RedisUtils redisUtils;
    private final ParamsAdapter paramsAdapter;
    private final AccountRoleService accountRoleService;

    @Override
    public LoginResponse login(LoginRequest request) {
        if (FuncUtils.isEmpty(request.getUsername()) || FuncUtils.isEmpty(request.getPassword())) {
            throw new RuntimeException("用户名和密码不能为空");
        }

        // 查询用户（支持用户名或邮箱登录）
        Account account = this.lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(Account::getUsername, request.getUsername())
                        .or()
                        .eq(Account::getEmail, request.getUsername())
                )
                .one();

        if (account == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查用户是否被禁用
        if (Boolean.FALSE.equals(account.getEnabled())) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // 验证密码
        String salt = account.getId().toString();
        String encryptedPassword = CryptoUtils.md5(request.getPassword(), salt);
        if (!encryptedPassword.equals(account.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 生成token
        return generateTokenResponse(account);
    }

    @Override
    public boolean register(RegisterRequest request) {
        // 参数校验
        validateRegisterRequest(request);

        // 检查用户名是否已存在
        if (this.lambdaQuery().eq(Account::getUsername, request.getUsername()).exists()) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (this.lambdaQuery().eq(Account::getEmail, request.getEmail()).exists()) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建账号
        Account account = new Account();
        account.setNickname(request.getNickname());
        account.setEmail(request.getEmail());
        account.setUsername(request.getUsername());
        account.setEnabled(true);

        // 保存以获取ID
        this.save(account);

        // 加密密码（使用用户ID作为盐值）
        String salt = account.getId().toString();
        String encryptedPassword = CryptoUtils.md5(request.getPassword(), salt);
        account.setPassword(encryptedPassword);

        // 更新密码
        this.updateById(account);

        AuthInterceptor.setUserRole(account.getId(), Role.READ_ONLY);

        // 设置角色
        return accountRoleService.save(AccountRole.builder().accountId(account.getId()).role(Role.READ_ONLY).build());
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        if (FuncUtils.isEmpty(request.getRefreshToken())) {
            throw new RuntimeException("refreshToken不能为空");
        }

        try {
            // 解析refreshToken
            Claims claims = TokenUtils.parseToken(request.getRefreshToken());
            String userId = claims.getId();

            // 查询用户
            Account account = this.getById(userId);
            if (account == null) {
                throw new RuntimeException("用户不存在");
            }

            // 检查用户是否被禁用
            if (Boolean.FALSE.equals(account.getEnabled())) {
                throw new RuntimeException("账号已被禁用，请联系管理员");
            }

            // 生成新的token
            return generateTokenResponse(account);
        } catch (Exception e) {
            throw new NotAuthException("refreshToken无效或已过期");
        }
    }

    @Override
    public void logout() {
        try {
            redisUtils.delete(SysConst.LOGIN_USER_KEY + TokenUtils.getToken());
        } catch (Exception e) {
            throw new RuntimeException("退出登录失败");
        }
    }

    @Override
    public boolean changePassword(ChangePasswordRequest request) {
        if (FuncUtils.isEmpty(request.getOldPassword()) || FuncUtils.isEmpty(request.getNewPassword())) {
            throw new RuntimeException("旧密码和新密码不能为空");
        }

        // 获取当前用户ID
        Long userId = UserUtils.getId();
        Account account = this.getById(userId);
        if (account == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        String salt = account.getId().toString();
        String encryptedOldPassword = CryptoUtils.md5(request.getOldPassword(), salt);
        if (!encryptedOldPassword.equals(account.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        // 加密新密码
        String encryptedNewPassword = CryptoUtils.md5(request.getNewPassword(), salt);
        account.setPassword(encryptedNewPassword);

        return this.updateById(account);
    }

    @Override
    public boolean updateProfile(UpdateProfileRequest request) {
        // 获取当前用户ID
        Long userId = UserUtils.getId();
        Account account = this.getById(userId);
        if (account == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新昵称
        if (!FuncUtils.isEmpty(request.getNickname())) {
            account.setNickname(request.getNickname());
        }

        // 更新邮箱
        if (!FuncUtils.isEmpty(request.getEmail())) {
            // 检查邮箱是否已被其他用户使用
            if (this.lambdaQuery()
                    .eq(Account::getEmail, request.getEmail())
                    .ne(Account::getId, userId)
                    .exists()) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
            account.setEmail(request.getEmail());
        }

        return this.updateById(account);
    }

    @Override
    public boolean toggleEnabled(Long id, Boolean enabled) {
        Account account = this.getById(id);

        if (account == null) {
            throw new RuntimeException("用户不存在");
        }

        if (Objects.equals(account.getId(), SysConst.ADMIN_ACCOUNT_ID)) {
            throw new RuntimeException("管理员账号不可操作");
        }

        account.setEnabled(enabled);
        return this.updateById(account);
    }

    @Override
    public boolean adminChangePassword(Long id, String newPassword) {
        if (FuncUtils.isEmpty(newPassword)) {
            throw new RuntimeException("新密码不能为空");
        }

        Account account = this.getById(id);
        if (account == null) {
            throw new RuntimeException("用户不存在");
        }

        // 加密新密码
        String salt = account.getId().toString();
        String encryptedPassword = CryptoUtils.md5(newPassword, salt);
        account.setPassword(encryptedPassword);

        return this.updateById(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeRole(Long id, List<Role> roles) {
        Account account = this.getById(id);
        if (account == null) {
            throw new RuntimeException("账号不存在");
        }

        if (!account.getEnabled()) {
            throw new RuntimeException("未激活账号不可设置");
        }

        if (Objects.equals(account.getId(), SysConst.ADMIN_ACCOUNT_ID)) {
            throw new RuntimeException("管理员账号不可操作");
        }

        accountRoleService.remove(Wrappers.<AccountRole>lambdaQuery().eq(AccountRole::getAccountId, id));
        List<AccountRole> accountRoles = roles.stream()
                .map((role) -> AccountRole.builder().accountId(id).role(role).build())
                .toList();

        accountRoleService.saveBatch(accountRoles);

        AuthInterceptor.setUserRole(id, roles.getFirst());
        senRoleChangeNoticeUseWs(String.valueOf(id), roles.getFirst());

        return true;
    }

    /**
     * 生成Token响应
     *
     * @param account 账号信息
     * @return 登录响应
     */
    private LoginResponse generateTokenResponse(Account account) {
        String userId = account.getId().toString();

        // 构建UserDetail
        UserDetail userDetail = UserDetail.builder()
                .id(account.getId())
                .username(account.getUsername())
                .name(account.getNickname())
                .email(account.getEmail())
                .build();

        // 生成 accessToken 和 refreshToken
        long accessTokenTtl = Long.parseLong(paramsAdapter.getValue("ACCESS_TOKEN_TTL"));
        String accessToken = TokenUtils.createToken(userId, userDetail, accessTokenTtl);
        long refreshTokenTtl = Long.parseLong(paramsAdapter.getValue("REFRESH_TOKEN_TTL"));
        String refreshToken = TokenUtils.createToken(userId, userDetail, refreshTokenTtl);

        // 存储到Redis
        String userDetailStr = JsonUtils.toJsonStr(userDetail);
        redisUtils.setEx(SysConst.LOGIN_USER_KEY + accessToken, userDetailStr, accessTokenTtl, TimeUnit.MILLISECONDS);

        // 返回登录响应
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userDetail(userDetail)
                .build();
    }

    /**
     * 校验注册请求参数
     *
     * @param request 注册请求
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (FuncUtils.isEmpty(request.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        if (FuncUtils.isEmpty(request.getPassword())) {
            throw new RuntimeException("密码不能为空");
        }
        if (FuncUtils.isEmpty(request.getEmail())) {
            throw new RuntimeException("邮箱不能为空");
        }
        if (FuncUtils.isEmpty(request.getNickname())) {
            throw new RuntimeException("昵称不能为空");
        }
    }

    /**
     * 使用ws发送通知给前端
     * @param accountId 账号ID
     * @param role 新的角色
     */
    private void senRoleChangeNoticeUseWs(String accountId, Role role) {
        try {
            List<ApboaWebSocketSession> sessions = ApboaWebSocketSessionManager.getSessionByAccountId(accountId);
            ServerMessageHandler messageHandler = ServiceMessageHandlerAdapter.getHandler(WsMessageType.ACCOUNT_ROLE_CHANGE);
            sessions.forEach(session -> {
                WsServerMessage message = new WsServerMessage(
                        WsMessageType.ACCOUNT_ROLE_CHANGE,
                        AccountRoleChangeMessage
                                .builder()
                                .accountId(accountId)
                                .role(role)
                                .build());

                messageHandler.handle(session, message);
            });
        } catch (Exception ignored) {}
    }
}
