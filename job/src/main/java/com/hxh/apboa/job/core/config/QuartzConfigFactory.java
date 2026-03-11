package com.hxh.apboa.job.core.config;

import com.hxh.apboa.job.core.enums.QuartzEnum;
import com.hxh.apboa.job.core.job.QuartzJob;

/**
 * 描述：QuartzConfig 工厂
 * 这是定时任务的语法糖，用于创建配置和启动任务
 *
 * @author huxuehao
 **/
public class QuartzConfigFactory {
    private final QuartzConfig config = new QuartzConfig();
    public QuartzConfigFactory identity(String identity) {
        config.setIdentity(identity);
        config.putDataMap(QuartzEnum.IDENTITY_KEY.value(), identity);
        return this;
    }
    public QuartzConfigFactory setJobClass(Class<? extends QuartzJob> clazz) {
        config.setJobClass(clazz);
        return this;
    }
    public QuartzConfigFactory putDataMap(String key, Object val) {
        config.putDataMap(key, val);
        return this;
    }

    public QuartzConfigFactory once() {
        config.setCron(null);
        return this;
    }

    public QuartzConfigFactory cron(String cron) {
        config.setCron(cron);
        return this;
    }
    public QuartzConfig build() {
        config.checkIdentity();
        config.checkJobClass();
        return config;
    }
}
