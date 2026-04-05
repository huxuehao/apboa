package com.hxh.apboa.common.consts;

/**
 * 描述：RedisChannelTopic
 *
 * @author huxuehao
 **/
public class RedisChannelTopic {
    public static final String AGENT_REREGISTER_CHANNEL = "apboa:agent:cluster:reRegister";
    public static final String AGENT_UNREGISTER_CHANNEL = "apboa:agent:cluster:unRegister";
    public static final String SK_SYNC_CHANNEL = "apboa:sk:sync";
    public static final String JOB_CLUSTER_CONTROL = "apboa:job:cluster:control";
    public static final String WS_CHANNEL_PATTERN = "apboa:ws:cluster:*";
}
