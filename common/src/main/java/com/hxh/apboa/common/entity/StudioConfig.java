package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.consts.TableConst;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Studio配置
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(TableConst.STUDIO_CONFIG)
@AllArgsConstructor
@NoArgsConstructor
public class StudioConfig implements SerializableEnable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String url;
    private String project;
}
