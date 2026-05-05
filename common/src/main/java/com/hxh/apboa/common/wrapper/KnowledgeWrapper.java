package com.hxh.apboa.common.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.RAGMode;
import lombok.*;

/**
 * 描述：Knowledge 包装类
 *
 * @author huxuehao
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeWrapper {
    private RAGMode ragMode;
    private Knowledge knowledge;
    private JsonNode retrievalConfig;
}
