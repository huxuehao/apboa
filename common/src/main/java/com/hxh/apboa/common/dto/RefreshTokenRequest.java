package com.hxh.apboa.common.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Token刷新请求DTO
 *
 * @author huxuehao
 */
@Data
public class RefreshTokenRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 刷新令牌
     */
    private String refreshToken;
}
