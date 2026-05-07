package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.consts.TableConst;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RAG文档分块表
 *
 * @author huxuehao
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(TableConst.RAG_DOCUMENT_CHUNK)
public class RagDocumentChunk implements SerializableEnable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long documentId;

    private String fileName;

    private Integer chunkIndex;

    private String content;

    private Integer tokenCount;

    private Integer startOffset;

    private Integer endOffset;

    private String metadata;

    private LocalDateTime createdAt;
}
