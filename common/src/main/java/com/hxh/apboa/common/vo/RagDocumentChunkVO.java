package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.entity.RagDocumentChunk;
import lombok.Getter;
import lombok.Setter;

/**
 * 描述：RagDocumentChunk VO
 *
 * @author huxuehao
 **/
@Getter
@Setter
public class RagDocumentChunkVO extends RagDocumentChunk {
    private double score = 0;
}
