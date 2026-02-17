package com.hxh.apboa.model.service.impl;

import com.hxh.apboa.common.entity.ModelConfig;
import com.hxh.apboa.common.entity.ModelProvider;
import com.hxh.apboa.model.mapper.ModelConfigMapper;
import com.hxh.apboa.model.mapper.ModelProviderMapper;
import com.hxh.apboa.model.service.ModelProviderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型提供商Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class ModelProviderServiceImpl extends ServiceImpl<ModelProviderMapper, ModelProvider> implements ModelProviderService {
    private final ModelConfigMapper modelConfigMapper;

    @Override
    public List<Object> usedWithModel(List<Long> ids) {
        List<Object> names = new ArrayList<>();

        LambdaQueryWrapper<ModelConfig> qw = new QueryWrapper<ModelConfig>().lambda().in(ModelConfig::getProviderId, ids);
        modelConfigMapper.selectList(qw).forEach(modelConfig -> {
            names.add(modelConfig.getName());
        });

        return names;
    }
}
