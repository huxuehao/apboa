package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 健康状态
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum HealthStatus {
    HEALTHY("健康"),
    UNHEALTHY("不健康"),
    UNKNOWN("未知");

    private final String description;
}
