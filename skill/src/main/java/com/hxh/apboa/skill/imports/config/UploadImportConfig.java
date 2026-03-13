package com.hxh.apboa.skill.imports.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 描述：上传导入配置
 *
 * @author huxuehao
 **/
@Getter
@Setter
@Builder
public class UploadImportConfig {
    /**
     * 类型
     */
    private String category;
    /**
     * 是否覆盖
     */
    private boolean cover;
    /**
     * 临时路径
     */
    private String templatePath;
}
