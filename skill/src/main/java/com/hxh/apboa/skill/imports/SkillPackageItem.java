package com.hxh.apboa.skill.imports;

import lombok.Builder;
import lombok.Data;

/**
 * 描述：SkillPackageItem
 *
 * @author huxuehao
 **/
@Data
@Builder
public class SkillPackageItem {
    private String prefix;
    private String name;
    private String content;
}
