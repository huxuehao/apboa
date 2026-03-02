package com.hxh.apboa.params.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.common.entity.Params;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.params.core.ParamsAdapter;
import com.hxh.apboa.params.mapper.ParamsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 描述：系统参数
 *
 * @author huxuehao
 **/
@Service
public class ParamsServiceImpl extends ServiceImpl<ParamsMapper, Params> implements ParamsService {
    private final ParamsAdapter paramsAdapter;

    public ParamsServiceImpl(ParamsAdapter paramsAdapter) {
        this.paramsAdapter = paramsAdapter;
    }

    @Override
    public String fetchValueByKey(String key) {
        return paramsAdapter.getValue(key);
    }

    @Override
    public boolean saveV2(Params params) {
        if(FuncUtils.isEmpty(params.getParamValue())) {
            throw new RuntimeException("value值不可为空");
        }

        List<Params> list = lambdaQuery()
                .eq(Params::getParamKey, params.getParamKey())
                .list();
        if (list == null || list.isEmpty()) {
            return paramsAdapter.saveParams(params) > 0;
        } else {
            throw new RuntimeException("Key已存在");
        }
    }


    @Override
    public boolean updateByIdV2(Params params) {
        if(FuncUtils.isEmpty(params.getParamValue())) {
            throw new RuntimeException("value值不可为空");
        }

        List<Params> list = lambdaQuery()
                .eq(Params::getId, params.getId())
                .eq(Params::getParamKey, params.getParamKey())
                .list();
        if (list == null || list.isEmpty()) {
            return paramsAdapter.updateParams(params) > 0;
        } else {
            throw new RuntimeException("Key已存在");
        }
    }
}
