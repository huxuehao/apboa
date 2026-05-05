package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.enums.RagDocumentStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RAG文档表
 *
 * @author huxuehao
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(TableConst.RAG_DOCUMENT)
public class RagDocument implements SerializableEnable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long knowledgeBaseConfigId;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private Integer chunkCount;

    private RagDocumentStatus status;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}
