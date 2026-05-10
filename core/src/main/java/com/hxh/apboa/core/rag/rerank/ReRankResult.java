package com.hxh.apboa.core.rag.rerank;

import java.io.Serializable;

/**
 * 重排序结果
 *
 * @author huxuehao
 */
public record ReRankResult(int index, double score) implements Serializable {
}
