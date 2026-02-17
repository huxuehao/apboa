package com.hxh.apboa.common.config.auth;

import com.hxh.apboa.common.enums.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 描述：需要的角色
 *
 * @author huxuehao
 **/
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleNeed {
    Role[] value();
}
