package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.UserDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录响应DTO
 *
 * @author huxuehao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 到期时间（毫秒）
     */
    private Long accessTokenTTL;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 到期时间（毫秒）
     */
    private Long refreshTokenTTL;

    /**
     * 用户详情
     */
    private UserDetail userDetail;
}
