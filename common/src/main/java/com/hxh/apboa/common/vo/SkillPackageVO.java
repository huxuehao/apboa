package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 技能包VO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode
public class SkillPackageVO implements SerializableEnable {
    private Long id;
    private String name;
    private String description;
    private String skillContent;
    private String category;
    private JsonNode references;
    private JsonNode examples;
    private JsonNode scripts;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<Object> used;
    private List<Long> tools;
}
