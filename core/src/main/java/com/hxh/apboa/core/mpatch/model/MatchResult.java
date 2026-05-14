package com.hxh.apboa.core.mpatch.model;

/**
 * 描述：匹配结果
 * 表示在代码中找到的一个匹配位置
 *
 * @author huxuehao
 **/
public class MatchResult {
    /**
     * 匹配位置（字符索引）
     */
    private final int index;

    /**
     * 匹配长度
     */
    private final int length;

    /**
     * 匹配的上下文（用于错误提示）
     */
    private final String context;

    /**
     * 是否唯一匹配（可变，用于后续更新）
     * -- SETTER --
     *  设置是否唯一匹配
     *  这个方法在去重后被调用，用于更新唯一性状态
     *
     */
    private boolean isUnique;

    public MatchResult(int index, int length, String context, boolean isUnique) {
        this.index = index;
        this.length = length;
        this.context = context;
        this.isUnique = isUnique;
    }

    public int getIndex() {
        return index;
    }

    public int getLength() {
        return length;
    }

    public String getContext() {
        return context;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }

    @Override
    public String toString() {
        return "MatchResult{" +
                "index=" + index +
                ", length=" + length +
                ", isUnique=" + isUnique +
                '}';
    }
}
