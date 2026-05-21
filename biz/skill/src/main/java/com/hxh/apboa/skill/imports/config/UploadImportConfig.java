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
     * 解析后的 skills 根目录
     */
    private String templatePath;

    /**
     * 压缩包解压根目录（{@code .apboa/temp/{uuid}/}），用于导入完成后清理
     */
    private String extractDirPath;
}
