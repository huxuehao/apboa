package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.consts.TableConst;
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
@TableName(value = TableConst.SECRET_KEY)
public class SecretKey extends BaseEntity {

    /**
     * 名称
     */
    private String name;

    /**
     * 访问秘钥值
     */
    private String value;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 备注
     */
    private String remark;
}
