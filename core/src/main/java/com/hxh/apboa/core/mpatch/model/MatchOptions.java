package com.hxh.apboa.core.mpatch.model;

/**
 * 描述：匹配选项
 * 用于配置搜索匹配的行为
 *
 * @author huxuehao
 **/
public class MatchOptions {
    /**
     * 是否要求唯一匹配，默认 true
     * 如果为 true，当找到多个匹配时会返回错误
     */
    private final boolean requireUniqueMatch;

    /**
     * 上下文行数，默认 3
     * 用于在错误信息中显示匹配位置的上下文
     */
    private final int contextLines;

    /**
     * 是否允许模糊匹配，默认 true
     * 如果精确匹配失败，会尝试忽略空白差异进行模糊匹配
     */
    private final boolean allowFuzzyMatch;

    private MatchOptions(Builder builder) {
        this.requireUniqueMatch = builder.requireUniqueMatch;
        this.contextLines = builder.contextLines;
        this.allowFuzzyMatch = builder.allowFuzzyMatch;
    }

    /**
     * 获取默认选项
     */
    public static MatchOptions defaults() {
        return new Builder().build();
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isRequireUniqueMatch() {
        return requireUniqueMatch;
    }

    public int getContextLines() {
        return contextLines;
    }

    public boolean isAllowFuzzyMatch() {
        return allowFuzzyMatch;
    }

    @Override
    public String toString() {
        return "MatchOptions{" +
                "requireUniqueMatch=" + requireUniqueMatch +
                ", contextLines=" + contextLines +
                ", allowFuzzyMatch=" + allowFuzzyMatch +
                '}';
    }

    /**
     * Builder 模式构建器
     */
    public static class Builder {
        private boolean requireUniqueMatch = true;
        private int contextLines = 3;
        private boolean allowFuzzyMatch = true;

        public Builder requireUniqueMatch(boolean requireUniqueMatch) {
            this.requireUniqueMatch = requireUniqueMatch;
            return this;
        }

        public Builder contextLines(int contextLines) {
            this.contextLines = contextLines;
            return this;
        }

        public Builder allowFuzzyMatch(boolean allowFuzzyMatch) {
            this.allowFuzzyMatch = allowFuzzyMatch;
            return this;
        }

        public MatchOptions build() {
            return new MatchOptions(this);
        }
    }
}
