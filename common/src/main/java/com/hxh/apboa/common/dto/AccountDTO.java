package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.mp.support.QueryCondition;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账号查询DTO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountDTO extends PageParams {

    @QueryDefine(value = "昵称", condition = QueryCondition.LIKE)
    private String nickname;

    @QueryDefine(value = "邮箱", condition = QueryCondition.LIKE)
    private String email;

    @QueryDefine(value = "用户名", condition = QueryCondition.LIKE)
    private String username;

    @QueryDefine(value = "是否可用", condition = QueryCondition.EQ)
    private Boolean enabled;
}
