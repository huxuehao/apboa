package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 描述：角色
 *
 * @author huxuehao
 **/
@Getter
@AllArgsConstructor
public enum Role {
    READ_ONLY("只读"),
    EDIT("编辑"),
    ADMIN("管理员");

    private final String description;
}
