package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.enums.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账号VO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode
public class AccountVO implements SerializableEnable {
    private Long id;
    private String nickname;
    private String email;
    private String username;
    private Boolean enabled;
    private List<Role> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
