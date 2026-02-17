package com.hxh.apboa.common;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 描述：用户详情
 *
 * @author huxuehao
 **/
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetail implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    // 昵称
    private String name;
    // 账号
    private String username;
    // 邮箱
    private String email;
}
