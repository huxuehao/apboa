package com.hxh.apboa.common.wrapper;

import lombok.*;

/**
 * 描述：钩子配置包装类
 *
 * @author huxuehao
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HookConfigWrapper {
    String name;
    String description;
    String classPath;
}
