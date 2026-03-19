package com.hxh.apboa.skill.imports.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 描述：本地导入配置
 *
 * @author huxuehao
 **/
@Getter
@Setter
public class LocalImportConfig {
    /**
     * 类型
     */
    private String category;
    /**
     * 本地路径
     */
    private String path;
    /**
     * 是否覆盖
     */
    private boolean cover;
}
