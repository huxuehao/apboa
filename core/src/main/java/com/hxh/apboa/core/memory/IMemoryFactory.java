package com.hxh.apboa.core.memory;

import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 描述：记忆管理工厂
 *
 * @author huxuehao
 **/
public class IMemoryFactory {

    // 节点缓存，key 为节点唯一ID
    private static final Map<String, TreeNode> nodeMap = new ConcurrentHashMap<>();
    // 根节点缓存，key 为根节点的 threadId
    private static final Map<String, TreeNode> rootMap = new ConcurrentHashMap<>();

    // 全局锁，保证操作原子性
    private static final Object lock = new Object();

    /**
     * 树节点内部类
     */
    private static class TreeNode {
        String nodeId;
        Memory memory;
        List<TreeNode> children;

        TreeNode(String nodeId, Memory memory) {
            this.nodeId = nodeId;
            this.memory = memory;
            this.children = new CopyOnWriteArrayList<>();
        }
    }


    /**
     * 创建 AutoContextMemory
     */
    public static AutoContextMemory createAutoContextMemory(AutoContextConfig config, Model model, String nodeId, String parentNodeId) {

        return (AutoContextMemory) createMemory(nodeId, parentNodeId, new AutoContextMemory(config, model));
    }

    /**
     * 创建 InMemoryMemory
     */
    public static InMemoryMemory createInMemoryMemory(String nodeId, String parentNodeId) {

        return (InMemoryMemory) createMemory(nodeId, parentNodeId, new InMemoryMemory());
    }

    /**
     * 创建 Memory
     * @param nodeId 节点ID（唯一标识）
     * @param parentNodeId（父节点ID）
     * @param memory 记忆
     */
    public static Memory createMemory(String nodeId, String parentNodeId, Memory memory) {
        boolean isRoot = (parentNodeId == null);

        // 处理节点ID已存在的情况
        if (nodeMap.containsKey(nodeId)) {
            if (isRoot) {
                // 根节点重复：清理旧树并替换
                TreeNode oldRoot = rootMap.remove(nodeId);
                if (oldRoot != null) {
                    clearTree(oldRoot);
                }
            } else {
                // 子节点重复：抛出异常（业务上应保证唯一）
                throw new RuntimeException("Node with id " + nodeId + " already exists");
            }
        }

        // 创建内存对象和树节点
        TreeNode newNode = new TreeNode(nodeId, memory);

        if (isRoot) {
            // 清除已经存在的
            clear(nodeId);
            // 加入根缓存
            rootMap.put(nodeId, newNode);
        } else {
            // 查找父节点并建立关联
            TreeNode parentNode = nodeMap.get(parentNodeId);
            if (parentNode == null) {
                throw new RuntimeException("Parent node not found for id: " + parentNodeId);
            }
            parentNode.children.add(newNode);
        }
        // 统一加入节点缓存
        nodeMap.put(nodeId, newNode);

        return memory;
    }


    /**
     * 根据根节点 rootNodeId 清理整棵树
     *
     * @param rootNodeId 根节点ID
     */
    public static void clear(String rootNodeId) {
        synchronized (lock) {
            TreeNode root = rootMap.remove(rootNodeId);
            if (root != null) {
                clearTree(root);
            }
        }
    }

    /**
     * 清空所有树
     */
    public static void clearAll() {
        synchronized (lock) {
            // 复制根节点列表，避免遍历时被修改
            List<TreeNode> roots = new ArrayList<>(rootMap.values());
            for (TreeNode root : roots) {
                clearTree(root);
            }
            rootMap.clear(); // 确保根缓存清空
        }
    }

    /**
     * 递归清理节点及其所有子节点
     *
     * @param node 起始节点
     */
    private static void clearTree(TreeNode node) {
        if (node == null) return;
        // 先递归清理子节点
        for (TreeNode child : node.children) {
            clearTree(child);
        }
        // 清理当前节点
        node.memory.clear();
        nodeMap.remove(node.nodeId);
    }
}
