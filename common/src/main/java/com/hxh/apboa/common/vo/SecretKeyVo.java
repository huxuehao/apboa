package com.hxh.apboa.common.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 访问秘钥表
 *
 * @author huxuehao
 */
@Getter
@Setter
public class SecretKeyVo {
    private Long id;
    private String name;
    private String value;
    private LocalDateTime expireTime;
    private String remark;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
