package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.consts.TableConst;
import lombok.*;

/**
 * 代码执行环境配置实体类
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = TableConst.CODE_EXECUTION_CONFIG, autoResultMap = true)
public class CodeExecutionConfig  extends BaseEntity {
    /**
     * 配置名称，便于识别
     */
    private String configName;

    /**
     * 工作目录，空则使用临时目录
     */
    private String workDir;

    /**
     * 脚本上传目录，空则使用work_dir/skills
     */
    private String uploadDir;

    /**
     * 是否自动上传skill文件，0=false
     */
    private Boolean autoUpload;

    /**
     * 是否启用ShellCommandTool，0=false
     */
    private Boolean enableShell;

    /**
     * 是否启用ReadFileTool，0=false
     */
    private Boolean enableRead;

    /**
     * 是否启用WriteFileTool，0=false
     */
    private Boolean enableWrite;

    /**
     * 允许执行的命令，如 python3、bash
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode command;
}
