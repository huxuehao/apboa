package com.hxh.apboa.common.wrapper;

import com.hxh.apboa.common.entity.ModelConfig;
import com.hxh.apboa.common.entity.ModelProvider;
import lombok.*;

/**
 * 描述：模型包装类
 *
 * @author huxuehao
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelWrapper {
    private ModelConfig config;
    private ModelProvider provider;
}
