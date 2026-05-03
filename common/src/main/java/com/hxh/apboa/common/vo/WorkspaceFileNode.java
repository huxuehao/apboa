package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 描述：工作空间文件树节点 VO
 *
 * @author huxuehao
 **/
@Data
@EqualsAndHashCode
public class WorkspaceFileNode implements SerializableEnable {
    /**
     * 文件或文件夹名称
     */
    private String name;
    /**
     * 文件在工作空间中的相对路径
     */
    private String path;
    /**
     * 是否为目录
     */
    private boolean directory;
    /**
     * 文件全名（仅文件有效，含后缀）
     */
    private String fullName;
    /**
     * 文件后缀（仅文件有效，不含点号，如 "txt"）
     */
    private String extension;
    /**
     * 文件大小可读格式（仅文件有效，如 "1.5 MB"）
     */
    private String readableSize;
    /**
     * 子节点（仅目录有效）
     */
    private List<WorkspaceFileNode> children;
}
