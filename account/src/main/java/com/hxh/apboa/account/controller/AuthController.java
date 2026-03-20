package com.hxh.apboa.account.controller;

import com.hxh.apboa.account.service.AccountService;
import com.hxh.apboa.common.config.auth.PassAuth;
import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.*;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.r.R;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;

    /**
     * 用户登录
     */
    @PassAuth
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse res = accountService.login(request);
//        setCookie(response, res.getAccessToken());
        return R.data(res, "登录成功");
    }

    /**
     * 用户注册
     */
    @PassAuth
    @PostMapping("/register")
    public R<Boolean> register(@RequestBody RegisterRequest request) {
        boolean result = accountService.register(request);
        return R.data(result);
    }

    /**
     * 刷新Token
     */
    @PassAuth
    @PostMapping("/refresh-token")
    public R<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request, HttpServletResponse response) {
        LoginResponse res = accountService.refreshToken(request);
//        setCookie(response, res.getAccessToken());
        return R.data(res, "刷新成功");
    }

    /**
     * 用户退出
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        accountService.logout();
        return R.success("退出成功");
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public R<Boolean> changePassword(@RequestBody ChangePasswordRequest request) {
        boolean result = accountService.changePassword(request);
        return R.data(result);
    }

    /**
     * 修改个人信息
     */
    @PostMapping("/update-profile")
    public R<Boolean> updateProfile(@RequestBody UpdateProfileRequest request) {
        boolean result = accountService.updateProfile(request);
        return R.data(result);
    }

    /**
     * 管理员新增用户
     */
    @RoleNeed({Role.ADMIN})
    @PostMapping("/admin/create-account")
    public R<Boolean> adminCreateAccount(@RequestBody RegisterRequest request) {
        return R.data(accountService.register(request));
    }

//    private void setCookie(HttpServletResponse response, String accessToken) {
//        Cookie accessTokenCookie = new Cookie("apboa-access-token", accessToken);
//        accessTokenCookie.setHttpOnly(true);  // 防止XSS攻击
//        accessTokenCookie.setSecure(true);    // 仅HTTPS传输
//        accessTokenCookie.setPath("/");       // 整个应用有效
//        accessTokenCookie.setMaxAge(-1);      // -1 表示会话级Cookie，浏览器关闭前一直有效
//        response.addCookie(accessTokenCookie);
//    }
}
