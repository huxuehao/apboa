package com.hxh.apboa.common.config;

import java.io.Serial;
import java.io.Serializable;

/**
 * 描述：序列化接口
 *
 * @author huxuehao
 **/
public interface SerializableEnable extends Serializable {
    @Serial
    long serialVersionUID = 1L;
}
