package com.hxh.apboa.core.mpatch.model;

import java.util.Collections;
import java.util.List;

/**
 * 描述：增量更新结构
 * 表示一次增量更新操作，可以是 SEARCH/REPLACE 模式或完整代码模式
 *
 * @author huxuehao
 **/
public class IncrementalUpdate {

    /**
     * 更新类型枚举
     */
    public enum Type {
        /**
         * 搜索替换模式：通过 SEARCH/REPLACE 指令进行增量更新
         */
        SEARCH_REPLACE,

        /**
         * 完整代码模式：直接使用完整的新代码替换
         */
        FULL_CODE
    }

    private final Type type;
    private final List<SearchReplaceItem> updates;
    private final String fullCode;

    private IncrementalUpdate(Type type, List<SearchReplaceItem> updates, String fullCode) {
        this.type = type;
        this.updates = updates != null ? Collections.unmodifiableList(updates) : null;
        this.fullCode = fullCode;
    }

    /**
     * 创建搜索替换类型的增量更新
     *
     * @param updates 搜索替换项列表
     * @return 增量更新实例
     */
    public static IncrementalUpdate searchReplace(List<SearchReplaceItem> updates) {
        return new IncrementalUpdate(Type.SEARCH_REPLACE, updates, null);
    }

    /**
     * 创建完整代码类型的增量更新
     *
     * @param fullCode 完整代码内容
     * @return 增量更新实例
     */
    public static IncrementalUpdate fullCode(String fullCode) {
        return new IncrementalUpdate(Type.FULL_CODE, null, fullCode);
    }

    /**
     * 判断是否为搜索替换类型
     */
    public boolean isSearchReplace() {
        return type == Type.SEARCH_REPLACE;
    }

    /**
     * 判断是否为完整代码类型
     */
    public boolean isFullCode() {
        return type == Type.FULL_CODE;
    }

    public Type getType() {
        return type;
    }

    public List<SearchReplaceItem> getUpdates() {
        return updates;
    }

    public String getFullCode() {
        return fullCode;
    }

    @Override
    public String toString() {
        return "IncrementalUpdate{" +
                "type=" + type +
                ", updates=" + (updates != null ? updates.size() + " items" : "null") +
                ", fullCode=" + (fullCode != null ? fullCode.length() + " chars" : "null") +
                '}';
    }
}
