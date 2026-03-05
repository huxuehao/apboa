package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 描述：A2aType
 *
 * @author huxuehao
 **/
@Getter
@AllArgsConstructor
public enum A2aType {
    WELLKNOWN("wellknown"),
    NACOS("nacos");

    private final String description;
}
