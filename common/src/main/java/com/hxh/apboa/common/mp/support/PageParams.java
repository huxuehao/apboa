package com.hxh.apboa.common.mp.support;

import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 描述：分页参数
 *
 * @author huxuehao
 */
@Setter
@Getter
public class PageParams implements SerializableEnable {
    private Integer current;
    private Integer size;

    private List<String> asc;
    private List<String> desc;
}
