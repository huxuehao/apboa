package com.hxh.apboa.sk;

import com.hxh.apboa.common.config.auth.AuthInterceptor;
import com.hxh.apboa.sk.service.SecretKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 描述：初始化API Key
 *
 * @author huxuehao
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class SkInit implements ApplicationRunner {
    private final SecretKeyService secretKeyService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            secretKeyService.listAll().forEach(secretKey -> {
                AuthInterceptor.addSkId(secretKey.getId());
                log.info("API Key 初始化完成");
            });
        } catch (Exception e) {
            log.error("API Key 初始化失败", e);
            throw new RuntimeException(e);
        }
    }
}
