package com.hxh.apboa.skill.cluster;

import com.hxh.apboa.cluster.core.ChannelSubscriber;
import com.hxh.apboa.common.consts.RedisChannelTopic;
import com.hxh.apboa.common.message.ParamChangeMessage;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.skill.SkillFileSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Component;

/**
 * 描述：参数变更 Redis 消息订阅者（技能模块）
 * 监听参数变更广播，当变更目标为技能相关参数时清除对应缓存
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class SkillCacheSubscriber implements ChannelSubscriber {

    /** 技能扩展名白名单参数 Key */
    private static final String SKILL_EXTENSIONS_KEY = "SKILL_FILE_ALLOWED_EXTENSIONS";

    @Override
    public Topic getTopic() {
        return new ChannelTopic(RedisChannelTopic.PARAM_CHANGE_CHANNEL);
    }

    @Override
    public void onMessage(String channel, String message) {
        if (!channel.equals(RedisChannelTopic.PARAM_CHANGE_CHANNEL)) {
            return;
        }

        try {
            ParamChangeMessage msg = JsonUtils.parse(message, ParamChangeMessage.class);
            if (msg == null) {
                return;
            }

            // 仅处理技能相关参数
            if (SKILL_EXTENSIONS_KEY.equals(msg.getParamKey())) {
                SkillFileSystemService.clearExtensionCache();
                log.info("参数变更触发技能扩展名缓存清除 - paramKey: {}, fromNode: {}",
                        msg.getParamKey(), msg.getSourceNodeId());
            } else {
                log.debug("参数变更不涉及技能模块，跳过 - paramKey: {}", msg.getParamKey());
            }
        } catch (Exception e) {
            log.error("处理参数变更消息失败", e);
        }
    }
}
