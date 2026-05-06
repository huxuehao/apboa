package com.hxh.apboa.core.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hxh.apboa.common.entity.RagDocumentChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG文档分块Mapper
 *
 * @author huxuehao
 */
@Mapper
public interface RagDocumentChunkMapper extends BaseMapper<RagDocumentChunk> {
}
