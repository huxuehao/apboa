package com.hxh.apboa.common.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 描述：系统参数变更消息（Redis 跨节点广播用）
 * 任一节点对 Params 表进行增/改操作后广播，各个模块订阅者根据 paramKey 自行判断是否处理
 *
 * @author huxuehao
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParamChangeMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 发生变更的参数 Key */
    private String paramKey;

    /** 源节点 ID，用于日志追踪 */
    private String sourceNodeId;

    /**
     * 创建参数变更消息
     *
     * @param nodeId   节点 ID
     * @param paramKey 参数 Key
     * @return 参数变更消息
     */
    public static ParamChangeMessage create(String nodeId, String paramKey) {
        return ParamChangeMessage.builder()
                .sourceNodeId(nodeId)
                .paramKey(paramKey)
                .build();
    }
}
