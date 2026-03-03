package com.hxh.apboa.params.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hxh.apboa.common.entity.Params;
import com.hxh.apboa.params.mapper.ParamsMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：参数校验适配器
 *
 * @author huxuehao
 **/
@Component
public class ParamsAdapter {
    // 注册表
    private static final Map<String, ParamsCore> registryMap = new ConcurrentHashMap<>();

    private final ParamsMapper paramsMapper;
    public ParamsAdapter(ParamsMapper paramsMapper) {
        this.paramsMapper = paramsMapper;
    }

    public void register(String key, ParamsCore paramsCore) {
        assert key != null : "transferEnum 不可为空";
        registryMap.put(key, paramsCore);
    }

    public String getValue(String key) {
        QueryWrapper<Params> qw = new QueryWrapper<>();
        qw.eq("param_key", key);
        ParamsCore paramsCore = registryMap.get(key);
        try {
            Params params = paramsMapper.selectOne(qw);
            if (params == null) {
                return paramsCore == null ? null : paramsCore.getDefaultValue();
            }
            if (paramsCore == null) {
                return params.getParamValue();
            }
            return paramsCore.checkAndFormatValue(params.getParamValue());
        } catch (Exception e) {
            throw new RuntimeException("不存在唯一Key:" + key, e);
        }
    }
    public int saveParams(Params params) {
        ParamsCore paramsCore = registryMap.get(params.getParamKey());
        if (paramsCore != null) {
            params.setParamValue(paramsCore.checkAndFormatValue(params.getParamValue()));
        }
        return paramsMapper.insert(params);
    }

    public int updateParams(Params params) {
        ParamsCore paramsCore = registryMap.get(params.getParamKey());
        if (paramsCore != null) {
            params.setParamValue(paramsCore.checkAndFormatValue(params.getParamValue()));
        }
        return paramsMapper.update(
                new UpdateWrapper<Params>()
                        .eq("id", params.getId())
                        .set("param_value", params.getParamValue()));
    }
}
