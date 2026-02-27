package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.enums.HookType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hook配置VO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode
public class HookConfigVO implements SerializableEnable {
    private Long id;
    private String name;
    private HookType hookType;
    private String description;
    private String classPath;
    private String code;
    private Integer priority;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<Object> used;
}
