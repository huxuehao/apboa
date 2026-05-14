package com.hxh.apboa.core.mpatch.model;

/**
 * 描述：更新结果
 * 表示一次增量更新操作的执行结果
 *
 * @author huxuehao
 **/
public class UpdateResult {

    private final boolean success;
    private final String updatedCode;
    private final String error;
    private final IncrementalUpdate appliedUpdate;
    private final Integer matchCount;

    private UpdateResult(Builder builder) {
        this.success = builder.success;
        this.updatedCode = builder.updatedCode;
        this.error = builder.error;
        this.appliedUpdate = builder.appliedUpdate;
        this.matchCount = builder.matchCount;
    }

    /**
     * 创建成功结果的快捷方法
     */
    public static UpdateResult success(String updatedCode) {
        return new Builder()
                .success(true)
                .updatedCode(updatedCode)
                .build();
    }

    /**
     * 创建失败结果的快捷方法
     */
    public static UpdateResult failure(String originalCode, String error) {
        return new Builder()
                .success(false)
                .updatedCode(originalCode)
                .error(error)
                .matchCount(0)
                .build();
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getUpdatedCode() {
        return updatedCode;
    }

    public String getError() {
        return error;
    }

    public IncrementalUpdate getAppliedUpdate() {
        return appliedUpdate;
    }

    public Integer getMatchCount() {
        return matchCount;
    }

    @Override
    public String toString() {
        return "UpdateResult{" +
                "success=" + success +
                ", updatedCode=" + (updatedCode != null ? updatedCode.length() + " chars" : "null") +
                ", error='" + error + '\'' +
                ", matchCount=" + matchCount +
                '}';
    }

    /**
     * Builder 模式构建器
     */
    public static class Builder {
        private boolean success;
        private String updatedCode;
        private String error;
        private IncrementalUpdate appliedUpdate;
        private Integer matchCount;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder updatedCode(String updatedCode) {
            this.updatedCode = updatedCode;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder appliedUpdate(IncrementalUpdate appliedUpdate) {
            this.appliedUpdate = appliedUpdate;
            return this;
        }

        public Builder matchCount(Integer matchCount) {
            this.matchCount = matchCount;
            return this;
        }

        public UpdateResult build() {
            return new UpdateResult(this);
        }
    }
}
