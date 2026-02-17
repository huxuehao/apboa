package com.hxh.apboa.common.r;

import java.io.Serializable;

/**
 * 描述：响应code接口
 *
 * @author huxuehao
 */
public interface IResultCode extends Serializable {
    String getMessage();

    int getCode();
}
