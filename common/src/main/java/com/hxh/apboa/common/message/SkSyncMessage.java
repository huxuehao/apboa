package com.hxh.apboa.common.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 描述：SK ID同步消息
 * 用于多节点间同步SK ID的添加和删除操作
 *
 * @author huxuehao
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkSyncMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 同步操作类型
     */
    public enum SyncType {
        /**
         * 添加SK ID
         */
        ADD,
        /**
         * 移除SK ID
         */
        REMOVE
    }

    /**
     * 操作类型
     */
    private SyncType type;

    /**
     * SK ID列表
     */
    private List<Long> skIds;

    /**
     * 源节点ID，用于区分消息来源
     */
    private String sourceNodeId;

    /**
     * 创建添加类型的同步消息
     *
     * @param nodeId 节点ID
     * @param skId   SK ID
     * @return 同步消息
     */
    public static SkSyncMessage createAddMessage(String nodeId, Long skId) {
        return SkSyncMessage.builder()
                .type(SyncType.ADD)
                .skIds(List.of(skId))
                .sourceNodeId(nodeId)
                .build();
    }

    /**
     * 创建移除类型的同步消息
     *
     * @param nodeId 节点ID
     * @param skIds  SK ID列表
     * @return 同步消息
     */
    public static SkSyncMessage createRemoveMessage(String nodeId, List<Long> skIds) {
        return SkSyncMessage.builder()
                .type(SyncType.REMOVE)
                .skIds(skIds)
                .sourceNodeId(nodeId)
                .build();
    }
}
