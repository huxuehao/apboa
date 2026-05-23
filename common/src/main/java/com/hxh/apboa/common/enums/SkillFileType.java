package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 描述：技能包文件类型
 *
 * @author huxuehao
 **/
@Getter
@AllArgsConstructor
public enum SkillFileType {
    SKILL_MD("SKILL.md"),
    REFERENCES("参考资源"),
    EXAMPLES("示例"),
    SCRIPTS("脚本");

    private final String description;
}
