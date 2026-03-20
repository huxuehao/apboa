package com.hxh.apboa.core.studio;

import com.hxh.apboa.common.util.CryptoUtils;
import com.hxh.apboa.common.util.TokenUtils;
import com.hxh.apboa.common.util.UserUtils;
import io.agentscope.core.studio.StudioManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：StudioManagerUtils
 *
 * @author huxuehao
 **/
public class StudioManagerUtils {
    private static final List<String> INIT_ONCE_CACHE = new ArrayList<>();
    public static void initOnce(String url, String project, String agentCode) {
        String runName = calculateRunName(agentCode);
        synchronized (StudioManagerUtils.class) {
            if (INIT_ONCE_CACHE.contains(runName)) {
                return;
            }

            StudioManager.init()
                    .studioUrl(url)
                    .project(project)
                    .runName(runName)
                    .initialize()
                    .block();
            INIT_ONCE_CACHE.add(runName);
        }
    }

    private static String calculateRunName(String agentCode) {
        String account = UserUtils.getAccount();
        String token = TokenUtils.getToken();
        return account + "_" + agentCode + "_" + CryptoUtils.md5(token, "token");
    }
}
