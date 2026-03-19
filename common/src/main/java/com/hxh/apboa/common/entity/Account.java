package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.consts.TableConst;
import lombok.Getter;
import lombok.Setter;

/**
 * 账号表
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = TableConst.ACCOUNT)
public class Account extends BaseEntity {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
