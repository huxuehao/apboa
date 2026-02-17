package com.hxh.apboa.common.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改个人信息请求DTO
 *
 * @author huxuehao
 */
@Data
public class UpdateProfileRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;
}
