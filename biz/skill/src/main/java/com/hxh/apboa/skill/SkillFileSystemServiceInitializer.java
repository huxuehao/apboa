package com.hxh.apboa.skill;

import com.hxh.apboa.params.core.ParamsAdapter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 描述：桥接组件，将 Spring 管理的 ParamsAdapter 注入到 SkillFileSystemService 静态工具类
 *
 * @author huxuehao
 **/
@Component
public class SkillFileSystemServiceInitializer {

    private final ParamsAdapter paramsAdapter;

    public SkillFileSystemServiceInitializer(ParamsAdapter paramsAdapter) {
        this.paramsAdapter = paramsAdapter;
    }

    @PostConstruct
    void init() {
        SkillFileSystemService.setParamsAdapter(paramsAdapter);
    }
}
