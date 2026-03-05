package com.hxh.apboa.a2a.config;

import lombok.Data;

/**
 * 描述：NacosMap
 *
 * @author huxuehao
 **/
@Data
public class KvMap {
    public String key;
    public String value;
    // 如果是evn是true,则从evn中获取value,key就是evn的key
    public boolean evn;
}
