package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RAG文档处理状态
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum RagDocumentStatus {
    PENDING("待处理"),
    PROCESSING("处理中"),
    COMPLETED("已完成"),
    FAILED("失败");

    private final String description;
}
