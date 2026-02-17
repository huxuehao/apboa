package com.hxh.apboa.common.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录请求DTO
 *
 * @author huxuehao
 */
@Data
public class LoginRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名或邮箱
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
