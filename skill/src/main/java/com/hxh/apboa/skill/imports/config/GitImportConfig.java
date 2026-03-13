package com.hxh.apboa.skill.imports.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 描述：git 导入配置
 * 如果仓库中存在 skills/ 子目录，会优先从该目录加载，否则使用仓库根目录。
 *
 * @author huxuehao
 **/
@Getter
@Setter
public class GitImportConfig {
    /**
     * 类型
     */
    private String category;
    /**
     * 仓库地址
     */
    private String repoUrl;
    /**
     * 是否覆盖
     */
    private boolean cover;
}
